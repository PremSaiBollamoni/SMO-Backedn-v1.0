package com.cutm.smo.services;

import com.cutm.smo.dto.ProcessPlanDraftRequest;
import com.cutm.smo.dto.ProcessPlanResponse;
import com.cutm.smo.dto.ProcessPlanStepRequest;
import com.cutm.smo.dto.WorkflowEdge;
import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import com.cutm.smo.util.LoggingUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class ProcessPlanService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final RoutingRepository routingRepository;
    private final RoutingStepRepository routingStepRepository;
    private final OperationRepository operationRepository;
    private final ProductRepository productRepository;
    private final RoutingEdgeRepository routingEdgeRepository;
    private final ObjectMapper objectMapper;

    public ProcessPlanService(RoutingRepository routingRepository, RoutingStepRepository routingStepRepository,
            OperationRepository operationRepository, ProductRepository productRepository,
            RoutingEdgeRepository routingEdgeRepository) {
        this.routingRepository = routingRepository;
        this.routingStepRepository = routingStepRepository;
        this.operationRepository = operationRepository;
        this.productRepository = productRepository;
        this.routingEdgeRepository = routingEdgeRepository;
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    @Transactional
    public ProcessPlanResponse createDraftProcessPlan(Long productId, List<Map<String, Object>> stepsRaw,
            List<ProcessPlanDraftRequest.EdgeRequest> edgesRaw) {
        if (productId == null || productId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId must be a positive number");
        }
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId does not exist");
        }

        List<ProcessPlanStepRequest> steps = parseAndValidateStrictSteps(stepsRaw);

        // Auto-generate routing_id
        Long routingId = routingRepository.findMaxRoutingId() + 1;

        Routing draftRouting = new Routing();
        draftRouting.setRoutingId(routingId);
        draftRouting.setProductId(productId);
        draftRouting.setVersion(routingRepository.findMaxVersionByProductId(productId) + 1);
        draftRouting.setStatus(STATUS_DRAFT);
        draftRouting.setApprovalStatus(STATUS_UNDER_REVIEW);
        draftRouting.setApprovedBy(null);
        draftRouting.setApprovedAt(null);
        draftRouting.setPreviousRoutingId(null);
        draftRouting = routingRepository.save(draftRouting);

        // Create operations and steps, get back name->operationId map
        Map<String, Long> nameToOperationId = createOperationsAndSteps(draftRouting.getRoutingId(), steps);

        // Store explicit edges from frontend
        if (edgesRaw != null && !edgesRaw.isEmpty()) {
            storeEdges(draftRouting.getRoutingId(), edgesRaw, nameToOperationId);
        }

        return getProcessPlan(draftRouting.getRoutingId());
    }

    @Transactional
    public ProcessPlanResponse cloneDraftFromExisting(Long sourceRoutingId, Long productId) {
        Routing source = routingRepository.findById(sourceRoutingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source routing not found"));

        Long targetProductId = productId == null ? source.getProductId() : productId;
        if (targetProductId == null || targetProductId <= 0 || !productRepository.existsById(targetProductId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId does not exist");
        }

        List<RoutingStep> sourceSteps = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(sourceRoutingId);
        if (sourceSteps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source plan has no steps");
        }
        Set<Long> operationIds = sourceSteps.stream().map(RoutingStep::getOperationId).collect(Collectors.toSet());
        Map<Long, Operation> operationMap = operationRepository.findAllById(operationIds).stream()
                .collect(Collectors.toMap(Operation::getOperationId, o -> o));

        // Auto-generate new routing_id
        Long newRoutingId = routingRepository.findMaxRoutingId() + 1;

        Routing draftRouting = new Routing();
        draftRouting.setRoutingId(newRoutingId);
        draftRouting.setProductId(targetProductId);
        draftRouting.setVersion(routingRepository.findMaxVersionByProductId(targetProductId) + 1);
        draftRouting.setStatus(STATUS_DRAFT);
        draftRouting.setApprovalStatus(STATUS_UNDER_REVIEW);
        draftRouting.setPreviousRoutingId(sourceRoutingId);
        draftRouting.setApprovedBy(null);
        draftRouting.setApprovedAt(null);
        routingRepository.save(draftRouting);

        Long nextOperationId = operationRepository.findMaxOperationId() + 1;
        Long nextRoutingStepId = routingStepRepository.findMaxRoutingStepId() + 1;
        for (RoutingStep sourceStep : sourceSteps) {
            Operation sourceOp = operationMap.get(sourceStep.getOperationId());
            if (sourceOp == null) {
                continue;
            }
            Operation clonedOperation = new Operation();
            clonedOperation.setOperationId(nextOperationId++);
            clonedOperation.setName(sourceOp.getName());
            clonedOperation.setDescription(sourceOp.getDescription());
            clonedOperation.setSequence(sourceOp.getSequence());
            clonedOperation.setOperationType(sourceOp.getOperationType());
            clonedOperation.setStageGroup(sourceOp.getStageGroup());
            clonedOperation.setStandardTime(sourceOp.getStandardTime());
            operationRepository.save(clonedOperation);

            RoutingStep newStep = new RoutingStep();
            newStep.setRoutingStepId(nextRoutingStepId++);
            newStep.setRoutingId(newRoutingId);
            newStep.setOperationId(clonedOperation.getOperationId());
            newStep.setStageGroup(clonedOperation.getStageGroup());
            routingStepRepository.save(newStep);
        }

        return getProcessPlan(newRoutingId);
    }

    public ProcessPlanResponse getProcessPlan(Long routingId) {
        log.debug("[getProcessPlan] === START GET PROCESS PLAN ===");
        log.debug("[getProcessPlan] Fetching routing ID: {}", routingId);
        
        Optional<Routing> routingOpt = routingRepository.findById(routingId);
        if (routingOpt.isEmpty()) {
            log.warn("[getProcessPlan] Routing not found for ID: {}", routingId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Routing not found");
        }
        
        Routing routing = routingOpt.get();
        log.debug("[getProcessPlan] Found routing: ID={}, Status={}, ApprovalStatus={}, Version={}, ProductID={}", 
                routing.getRoutingId(), routing.getStatus(), routing.getApprovalStatus(), 
                routing.getVersion(), routing.getProductId());
        
        List<RoutingStep> steps = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(routingId);
        log.debug("[getProcessPlan] Found {} routing steps for routing ID: {}", steps.size(), routingId);
        
        Set<Long> operationIds = steps.stream().map(RoutingStep::getOperationId).collect(Collectors.toSet());
        Map<Long, Operation> operationById = operationRepository.findAllById(operationIds).stream()
                .collect(Collectors.toMap(Operation::getOperationId, o -> o));
        
        List<ProcessPlanResponse.OperationResponse> operations = new ArrayList<>();
        for (RoutingStep step : steps) {
            Operation op = operationById.get(step.getOperationId());
            if (op != null) {
                operations.add(toOperationResponse(op));
            }
        }
        operations.sort(Comparator.comparing(ProcessPlanResponse.OperationResponse::getSequence, Comparator.nullsLast(Integer::compareTo)));
        
        // Load stored edges from routing_edge table
        List<WorkflowEdge> edges = loadStoredEdges(routingId);
        log.debug("[getProcessPlan] Loaded {} edges for routing ID: {}", edges.size(), routingId);
        
        ProcessPlanResponse response = new ProcessPlanResponse();
        response.setRoutingId(routing.getRoutingId());
        response.setProductId(routing.getProductId());
        response.setVersion(routing.getVersion());
        response.setStatus(routing.getStatus());
        response.setApprovalStatus(routing.getApprovalStatus());
        response.setApprovedBy(routing.getApprovedBy());
        response.setApprovedAt(routing.getApprovedAt());
        response.setPreviousRoutingId(routing.getPreviousRoutingId());
        response.setOperations(operations);
        response.setEdges(edges);
        
        log.debug("[getProcessPlan] === END GET PROCESS PLAN === Returning response for routing ID: {}", routingId);
        return response;
    }

    /**
     * Load stored edges from routing_edge table and convert to WorkflowEdge DTOs.
     */
    private List<WorkflowEdge> loadStoredEdges(Long routingId) {
        List<RoutingEdge> storedEdges = routingEdgeRepository.findByRoutingIdOrderByEdgeIdAsc(routingId);
        List<WorkflowEdge> edges = new ArrayList<>();
        for (RoutingEdge re : storedEdges) {
            edges.add(new WorkflowEdge(
                re.getFromOperationId(),
                re.getToOperationId(),
                re.getFromName(),
                re.getToName(),
                re.getEdgeType()
            ));
        }
        log.info("[loadStoredEdges] Loaded {} edges for routing {}", edges.size(), routingId);
        return edges;
    }

    /**
     * Build explicit edges from routing table relationships.
     * Uses routing order and operation types to determine dependencies.
     * Enhanced to handle missing operation types and merge chains.
     */
    private List<WorkflowEdge> buildExplicitEdges(List<RoutingStep> steps, Map<Long, Operation> operationById) {
        List<WorkflowEdge> edges = new ArrayList<>();
        
        if (steps.isEmpty()) {
            return edges;
        }
        
        log.info("[buildExplicitEdges] ═══ BUILDING EDGES FOR {} OPERATIONS ═══", steps.size());
        
        // Build a map of operation_id to operation for quick lookup
        Map<Long, Operation> opMap = new HashMap<>(operationById);
        
        // Log all operations for debugging
        for (int i = 0; i < steps.size(); i++) {
            RoutingStep step = steps.get(i);
            Operation op = opMap.get(step.getOperationId());
            if (op != null) {
                log.debug("[buildExplicitEdges] [{}] {} (type={}, stage={})", 
                    i, op.getName(), op.getOperationType(), op.getStageGroup());
            }
        }
        
        // Process each step and determine its outgoing edges
        for (int i = 0; i < steps.size(); i++) {
            RoutingStep current = steps.get(i);
            Operation currentOp = opMap.get(current.getOperationId());
            
            if (currentOp == null) {
                log.warn("[buildExplicitEdges] ⚠ Operation not found for step {}", i);
                continue;
            }
            
            log.debug("[buildExplicitEdges] Processing: {}", currentOp.getName());
            
            // Determine outgoing edges based on operation type
            if (currentOp.isSequential()) {
                // Sequential: connect to next operation
                if (i + 1 < steps.size()) {
                    RoutingStep next = steps.get(i + 1);
                    Operation nextOp = opMap.get(next.getOperationId());
                    if (nextOp != null) {
                        if (nextOp.isParallelBranch()) {
                            // Branching: connect to all parallel ops in next stage
                            int nextStage = next.getStageGroup();
                            log.debug("[buildExplicitEdges] Sequential {} branches to stage {}", currentOp.getName(), nextStage);
                            for (int j = i + 1; j < steps.size(); j++) {
                                RoutingStep candidate = steps.get(j);
                                Operation candidateOp = opMap.get(candidate.getOperationId());
                                if (candidateOp != null && candidateOp.isParallelBranch() && candidate.getStageGroup() == nextStage) {
                                    edges.add(new WorkflowEdge(
                                        currentOp.getOperationId(),
                                        candidateOp.getOperationId(),
                                        currentOp.getName(),
                                        candidateOp.getName(),
                                        "branch"
                                    ));
                                    log.debug("[buildExplicitEdges]   ✓ BRANCH EDGE: {} -> {}", currentOp.getName(), candidateOp.getName());
                                } else if (candidate.getStageGroup() > nextStage) {
                                    break;
                                }
                            }
                        } else {
                            // Simple sequential
                            edges.add(new WorkflowEdge(
                                currentOp.getOperationId(),
                                nextOp.getOperationId(),
                                currentOp.getName(),
                                nextOp.getName(),
                                "sequential"
                            ));
                            log.debug("[buildExplicitEdges]   ✓ SEQUENTIAL EDGE: {} -> {}", currentOp.getName(), nextOp.getName());
                        }
                    }
                }
            } else if (currentOp.isParallelBranch()) {
                // Parallel branch: Check if this is actually a branch SOURCE (fan-out point)
                // or a branch TARGET (receives from fan-out)
                
                // If name contains "CREATION" or "BIN", it's likely a branch source
                boolean isBranchSource = currentOp.getName().contains("CREATION") || 
                                        currentOp.getName().contains("BIN") ||
                                        currentOp.getName().contains("PART_BIN");
                
                if (isBranchSource) {
                    // This is a fan-out point - connect to all parallel branches in next stage
                    log.debug("[buildExplicitEdges] Branch source {} - creating fan-out edges", currentOp.getName());
                    
                    if (i + 1 < steps.size()) {
                        RoutingStep next = steps.get(i + 1);
                        int nextStage = next.getStageGroup();
                        
                        for (int j = i + 1; j < steps.size(); j++) {
                            RoutingStep candidate = steps.get(j);
                            Operation candidateOp = opMap.get(candidate.getOperationId());
                            
                            // Connect to all parallel branches in next stage
                            if (candidateOp != null && candidateOp.isParallelBranch() && 
                                candidate.getStageGroup() == nextStage &&
                                !candidateOp.getName().contains("CREATION") &&
                                !candidateOp.getName().contains("BIN")) {
                                
                                edges.add(new WorkflowEdge(
                                    currentOp.getOperationId(),
                                    candidateOp.getOperationId(),
                                    currentOp.getName(),
                                    candidateOp.getName(),
                                    "fan_out"
                                ));
                                log.debug("[buildExplicitEdges]   ✓ FAN-OUT EDGE: {} -> {}", currentOp.getName(), candidateOp.getName());
                            } else if (candidate.getStageGroup() > nextStage) {
                                break;
                            }
                        }
                    }
                } else {
                    // This is a branch target - connect to corresponding merge
                    String branchSuffix = currentOp.getName();
                    if (branchSuffix.endsWith("_LINE")) {
                        branchSuffix = branchSuffix.substring(0, branchSuffix.length() - 5);
                    }
                    String expectedMergeName = "MERGE_" + branchSuffix;
                    
                    log.debug("[buildExplicitEdges] Processing parallel branch: {} -> looking for {}", currentOp.getName(), expectedMergeName);
                    
                    // Find matching merge
                    boolean foundMerge = false;
                    for (int j = i + 1; j < steps.size(); j++) {
                        RoutingStep candidate = steps.get(j);
                        Operation candidateOp = opMap.get(candidate.getOperationId());
                        if (candidateOp != null && candidateOp.isMerge()) {
                            log.debug("[buildExplicitEdges]   Checking merge candidate: {}", candidateOp.getName());
                            if (candidateOp.getName().equals(expectedMergeName)) {
                                edges.add(new WorkflowEdge(
                                    currentOp.getOperationId(),
                                    candidateOp.getOperationId(),
                                    currentOp.getName(),
                                    candidateOp.getName(),
                                    "merge"
                                ));
                                log.debug("[buildExplicitEdges]   ✓ EDGE ADDED: {} -> {}", currentOp.getName(), candidateOp.getName());
                                foundMerge = true;
                                break;
                            }
                        }
                    }
                    if (!foundMerge) {
                        log.warn("[buildExplicitEdges] ⚠ No merge found for {}", currentOp.getName());
                    }
                }
            } else if (currentOp.isMerge()) {
                // Merge: DO NOT auto-connect to next merge node
                // But DO connect to next non-merge sequential operation
                if (i + 1 < steps.size()) {
                    RoutingStep next = steps.get(i + 1);
                    Operation nextOp = opMap.get(next.getOperationId());
                    if (nextOp != null && !nextOp.isMerge()) {
                        // Connect merge to next sequential operation
                        edges.add(new WorkflowEdge(
                            currentOp.getOperationId(),
                            nextOp.getOperationId(),
                            currentOp.getName(),
                            nextOp.getName(),
                            "merge_to_sequential"
                        ));
                        log.debug("[buildExplicitEdges]   ✓ MERGE TO SEQUENTIAL: {} -> {}", currentOp.getName(), nextOp.getName());
                    }
                }
            } else {
                // Unknown or NULL operation type - treat as sequential fallback
                log.warn("[buildExplicitEdges] ⚠ Unknown operation type for {}, treating as sequential", currentOp.getName());
                if (i + 1 < steps.size()) {
                    RoutingStep next = steps.get(i + 1);
                    Operation nextOp = opMap.get(next.getOperationId());
                    if (nextOp != null) {
                        edges.add(new WorkflowEdge(
                            currentOp.getOperationId(),
                            nextOp.getOperationId(),
                            currentOp.getName(),
                            nextOp.getName(),
                            "sequential_fallback"
                        ));
                        log.debug("[buildExplicitEdges]   ✓ FALLBACK EDGE: {} -> {}", currentOp.getName(), nextOp.getName());
                    }
                }
            }
        }
        
        // Hardcoded edges for missing branch->merge mappings
        // These ensure COLLAR_CUFF_LINE and POCKET_PLACKET_LINE connect to their corresponding merges
        addHardcodedEdges(edges, opMap, steps);
        
        log.info("[buildExplicitEdges] ═══ TOTAL EDGES BUILT: {} ═══", edges.size());
        for (WorkflowEdge edge : edges) {
            log.info("[buildExplicitEdges]   {} -> {} ({})", edge.getFromName(), edge.getToName(), edge.getEdgeType());
        }
        
        return edges;
    }
    
    /**
     * Add hardcoded edges for branch->merge mappings that may not be detected by the main logic.
     * This ensures all parallel branches connect to their corresponding merge points.
     */
    private void addHardcodedEdges(List<WorkflowEdge> edges, Map<Long, Operation> opMap, List<RoutingStep> steps) {
        // Define explicit branch->merge mappings
        String[][] branchMergePairs = {
            {"COLLAR_CUFF_LINE", "MERGE_COLLAR"},
            {"POCKET_PLACKET_LINE", "MERGE_POCKET"},
            {"SLEEVE_LINE", "MERGE_SLEEVE"},
            {"BODY_LINE", "MERGE_BODY"}
        };
        
        for (String[] pair : branchMergePairs) {
            final String branchName = pair[0];
            final String mergeName = pair[1];
            
            // Find branch and merge operations
            Operation branchOp = null;
            Operation mergeOp = null;
            
            for (Operation op : opMap.values()) {
                if (op.getName().equals(branchName)) {
                    branchOp = op;
                }
                if (op.getName().equals(mergeName)) {
                    mergeOp = op;
                }
            }
            
            // If both exist and edge doesn't already exist, add it
            if (branchOp != null && mergeOp != null) {
                final Operation finalBranchOp = branchOp;
                final Operation finalMergeOp = mergeOp;
                
                boolean edgeExists = edges.stream()
                    .anyMatch(e -> e.getFromOperationId().equals(finalBranchOp.getOperationId()) 
                        && e.getToOperationId().equals(finalMergeOp.getOperationId()));
                
                if (!edgeExists) {
                    edges.add(new WorkflowEdge(
                        branchOp.getOperationId(),
                        mergeOp.getOperationId(),
                        branchName,
                        mergeName,
                        "merge"
                    ));
                    log.debug("[addHardcodedEdges] ✓ HARDCODED EDGE ADDED: {} -> {}", branchName, mergeName);
                }
            }
        }
        
        // Define merge convergence points - where all merges flow to
        // Find the convergence operation (typically SIDE_SEAM, ASSEMBLY, or similar)
        String[] mergeNames = {"MERGE_COLLAR", "MERGE_POCKET", "MERGE_SLEEVE", "MERGE_BODY"};
        String[] convergenceTargets = {"SIDE_SEAM", "ASSEMBLY", "FINAL_ASSEMBLY", "FINISHING"};
        
        // Find which convergence target exists
        Operation convergenceOp = null;
        for (String targetName : convergenceTargets) {
            for (Operation op : opMap.values()) {
                if (op.getName().equalsIgnoreCase(targetName)) {
                    convergenceOp = op;
                    break;
                }
            }
            if (convergenceOp != null) break;
        }
        
        // If convergence point found, connect all merge nodes to it
        if (convergenceOp != null) {
            for (String mergeName : mergeNames) {
                Operation mergeOp = null;
                for (Operation op : opMap.values()) {
                    if (op.getName().equals(mergeName)) {
                        mergeOp = op;
                        break;
                    }
                }
                
                if (mergeOp != null) {
                    final Operation finalMergeOp = mergeOp;
                    final Operation finalConvergenceOp = convergenceOp;
                    
                    boolean edgeExists = edges.stream()
                        .anyMatch(e -> e.getFromOperationId().equals(finalMergeOp.getOperationId()) 
                            && e.getToOperationId().equals(finalConvergenceOp.getOperationId()));
                    
                    if (!edgeExists) {
                        edges.add(new WorkflowEdge(
                            mergeOp.getOperationId(),
                            convergenceOp.getOperationId(),
                            mergeName,
                            convergenceOp.getName(),
                            "convergence"
                        ));
                        log.debug("[addHardcodedEdges] ✓ CONVERGENCE EDGE ADDED: {} -> {}", mergeName, convergenceOp.getName());
                    }
                }
            }
            
            // Add sequential tail chain after convergence point
            // Find convergence operation in steps list
            int convergenceIndex = -1;
            for (int i = 0; i < steps.size(); i++) {
                Operation op = opMap.get(steps.get(i).getOperationId());
                if (op != null && op.getOperationId().equals(convergenceOp.getOperationId())) {
                    convergenceIndex = i;
                    break;
                }
            }
            
            // Create sequential chain from convergence point to end
            if (convergenceIndex >= 0) {
                for (int i = convergenceIndex; i < steps.size() - 1; i++) {
                    Operation currentOp = opMap.get(steps.get(i).getOperationId());
                    Operation nextOp = opMap.get(steps.get(i + 1).getOperationId());
                    
                    if (currentOp != null && nextOp != null) {
                        final Operation finalCurrentOp = currentOp;
                        final Operation finalNextOp = nextOp;
                        
                        boolean edgeExists = edges.stream()
                            .anyMatch(e -> e.getFromOperationId().equals(finalCurrentOp.getOperationId()) 
                                && e.getToOperationId().equals(finalNextOp.getOperationId()));
                        
                        if (!edgeExists) {
                            edges.add(new WorkflowEdge(
                                currentOp.getOperationId(),
                                nextOp.getOperationId(),
                                currentOp.getName(),
                                nextOp.getName(),
                                "sequential_tail"
                            ));
                            log.debug("[addHardcodedEdges] ✓ SEQUENTIAL TAIL EDGE: {} -> {}", currentOp.getName(), nextOp.getName());
                        }
                    }
                }
            }
        }
    }

    public List<ProcessPlanResponse> getProcessPlansByProduct(Long productId) {
        log.debug("[getProcessPlansByProduct] === START GET PROCESS PLANS BY PRODUCT ===");
        log.debug("[getProcessPlansByProduct] Product ID: {}", productId);
        
        List<Routing> routings = routingRepository.findByProductIdOrderByRoutingIdDesc(productId);
        log.debug("[getProcessPlansByProduct] Found {} routings for product ID: {}", routings.size(), productId);
        
        for (Routing r : routings) {
            log.debug("[getProcessPlansByProduct] Routing: ID={}, Status={}, ApprovalStatus={}, Version={}", 
                    r.getRoutingId(), r.getStatus(), r.getApprovalStatus(), r.getVersion());
        }
        
        List<ProcessPlanResponse> responses = new ArrayList<>();
        for (Routing routing : routings) {
            responses.add(getProcessPlan(routing.getRoutingId()));
        }
        
        log.debug("[getProcessPlansByProduct] === END GET PROCESS PLANS BY PRODUCT === Returning {} responses", responses.size());
        return responses;
    }

    public List<ProcessPlanResponse> getPendingProcessPlans() {
        List<Routing> pendingRoutings = routingRepository.findByApprovalStatusOrderByRoutingIdDesc(STATUS_UNDER_REVIEW);
        
        List<ProcessPlanResponse> responses = new ArrayList<>();
        for (Routing routing : pendingRoutings) {
            responses.add(getProcessPlan(routing.getRoutingId()));
        }
        return responses;
    }

    public List<ProcessPlanResponse> getApprovedProcessPlans() {
        log.debug("[getApprovedProcessPlans] === START GET APPROVED PROCESS PLANS ===");
        
        List<Routing> approved = routingRepository.findByStatusOrderByRoutingIdDesc(STATUS_APPROVED);
        log.debug("[getApprovedProcessPlans] Found {} approved routings", approved.size());
        
        for (Routing r : approved) {
            log.debug("[getApprovedProcessPlans] Routing: ID={}, Status={}, ApprovalStatus={}, Version={}, ProductID={}", 
                    r.getRoutingId(), r.getStatus(), r.getApprovalStatus(), r.getVersion(), r.getProductId());
        }
        
        List<ProcessPlanResponse> responses = new ArrayList<>();
        for (Routing routing : approved) {
            responses.add(getProcessPlan(routing.getRoutingId()));
        }
        
        log.debug("[getApprovedProcessPlans] === END GET APPROVED PROCESS PLANS === Returning {} responses", responses.size());
        return responses;
    }

    @Transactional
    public ProcessPlanResponse approveProcessPlan(Long routingId, Long approvedBy) {
        log.debug("[approveProcessPlan] === START APPROVAL PROCESS ===");
        log.debug("[approveProcessPlan] Routing ID: {}, Approved By: {}", routingId, approvedBy);
        
        Routing draftRouting = routingRepository.findById(routingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft routing not found"));
        
        log.debug("[approveProcessPlan] Found routing: ID={}, Status={}, ApprovalStatus={}, Version={}, ProductID={}", 
                draftRouting.getRoutingId(), draftRouting.getStatus(), draftRouting.getApprovalStatus(), 
                draftRouting.getVersion(), draftRouting.getProductId());

        if (!STATUS_UNDER_REVIEW.equalsIgnoreCase(draftRouting.getApprovalStatus())
                && !STATUS_DRAFT.equalsIgnoreCase(draftRouting.getStatus())) {
            log.warn("[approveProcessPlan] Invalid status for approval. Current status: {}, ApprovalStatus: {}", 
                    draftRouting.getStatus(), draftRouting.getApprovalStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only draft/pending plans can be approved");
        }
        if (approvedBy == null || approvedBy <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "approvedBy must be a positive number");
        }

        List<RoutingStep> draftSteps = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(routingId);
        log.debug("[approveProcessPlan] Found {} routing steps", draftSteps.size());
        if (draftSteps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft plan has no steps");
        }

        // Simply update the draft routing to APPROVED status - no new routing needed
        log.debug("[approveProcessPlan] Updating routing status to APPROVED");
        draftRouting.setApprovalStatus(STATUS_APPROVED);
        draftRouting.setStatus(STATUS_APPROVED);
        draftRouting.setApprovedBy(approvedBy);
        draftRouting.setApprovedAt(LocalDateTime.now());
        draftRouting.setPreviousRoutingId(null); // No previous routing for approved
        
        log.debug("[approveProcessPlan] Before save - Routing: ID={}, Status={}, ApprovalStatus={}", 
                draftRouting.getRoutingId(), draftRouting.getStatus(), draftRouting.getApprovalStatus());
        
        Routing savedRouting = routingRepository.save(draftRouting);
        log.debug("[approveProcessPlan] After save - Routing: ID={}, Status={}, ApprovalStatus={}", 
                savedRouting.getRoutingId(), savedRouting.getStatus(), savedRouting.getApprovalStatus());

        log.info("[approveProcessPlan] Process plan approved successfully. Routing ID: {}, Approved by: {}", routingId, approvedBy);
        log.debug("[approveProcessPlan] === END APPROVAL PROCESS ===");
        
        ProcessPlanResponse response = getProcessPlan(routingId);
        log.debug("[approveProcessPlan] Returning response for routing ID: {}", routingId);
        return response;
    }

    @Transactional
    public ProcessPlanResponse rejectProcessPlan(Long routingId, Long approvedBy) {
        log.debug("[rejectProcessPlan] === START REJECT PROCESS PLAN ===");
        log.debug("[rejectProcessPlan] Routing ID: {}, Rejected By: {}", routingId, approvedBy);
        
        Routing draftRouting = routingRepository.findById(routingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft routing not found"));

        log.debug("[rejectProcessPlan] Found routing: ID={}, Status={}, ApprovalStatus={}", 
                draftRouting.getRoutingId(), draftRouting.getStatus(), draftRouting.getApprovalStatus());

        if (approvedBy == null || approvedBy <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "approvedBy must be a positive number");
        }
        if (!STATUS_UNDER_REVIEW.equalsIgnoreCase(draftRouting.getApprovalStatus())
                && !STATUS_DRAFT.equalsIgnoreCase(draftRouting.getStatus())) {
            log.warn("[rejectProcessPlan] Invalid status for rejection. Current status: {}, ApprovalStatus: {}", 
                    draftRouting.getStatus(), draftRouting.getApprovalStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only draft/pending plans can be rejected");
        }

        log.debug("[rejectProcessPlan] Updating routing status to REJECTED");
        draftRouting.setApprovalStatus(STATUS_REJECTED);
        draftRouting.setApprovedBy(approvedBy);
        draftRouting.setApprovedAt(LocalDateTime.now());
        draftRouting.setStatus(STATUS_REJECTED);
        
        Routing savedRouting = routingRepository.save(draftRouting);
        log.debug("[rejectProcessPlan] After save - Routing: ID={}, Status={}, ApprovalStatus={}", 
                savedRouting.getRoutingId(), savedRouting.getStatus(), savedRouting.getApprovalStatus());
        
        log.info("[rejectProcessPlan] Process plan rejected successfully. Routing ID: {}", routingId);
        log.debug("[rejectProcessPlan] === END REJECT PROCESS PLAN ===");
        
        return getProcessPlan(draftRouting.getRoutingId());
    }

    private Map<String, Long> createOperationsAndSteps(Long routingId, List<ProcessPlanStepRequest> steps) {
        Long nextOperationId = operationRepository.findMaxOperationId() + 1;
        Long nextRoutingStepId = routingStepRepository.findMaxRoutingStepId() + 1;

        Map<String, Long> nameToOperationId = new HashMap<>();

        for (ProcessPlanStepRequest step : steps) {
            Operation operation = new Operation();
            operation.setOperationId(nextOperationId);
            operation.setName(step.getName().trim());
            operation.setDescription(step.getDescription().trim());
            operation.setSequence(step.getSequence());
            operation.setOperationType(step.getOperationType());
            operation.setStageGroup(step.getStageGroup());
            operation.setStandardTime(step.getStandardTime());
            operation = operationRepository.save(operation);

            nameToOperationId.put(operation.getName(), operation.getOperationId());

            RoutingStep routingStep = new RoutingStep();
            routingStep.setRoutingStepId(nextRoutingStepId++);
            routingStep.setRoutingId(routingId);
            routingStep.setOperationId(operation.getOperationId());
            routingStep.setStageGroup(operation.getStageGroup());
            routingStepRepository.save(routingStep);

            nextOperationId++;
        }

        return nameToOperationId;
    }

    /**
     * Store explicit edges from frontend into routing_edge table.
     */
    private void storeEdges(Long routingId, List<ProcessPlanDraftRequest.EdgeRequest> edgesRaw,
            Map<String, Long> nameToOperationId) {
        Long nextEdgeId = routingEdgeRepository.findMaxEdgeId() + 1;

        log.info("[storeEdges] Storing {} edges for routing {}", edgesRaw.size(), routingId);

        for (ProcessPlanDraftRequest.EdgeRequest edgeReq : edgesRaw) {
            String fromName = edgeReq.getFrom_name();
            String toName = edgeReq.getTo_name();
            String edgeType = edgeReq.getEdge_type() != null ? edgeReq.getEdge_type() : "SEQUENTIAL";

            Long fromOpId = nameToOperationId.get(fromName);
            Long toOpId = nameToOperationId.get(toName);

            if (fromOpId == null) {
                log.warn("[storeEdges] Skipping edge: from_name '{}' not found in operations", fromName);
                continue;
            }
            if (toOpId == null) {
                log.warn("[storeEdges] Skipping edge: to_name '{}' not found in operations", toName);
                continue;
            }

            RoutingEdge edge = new RoutingEdge();
            edge.setEdgeId(nextEdgeId++);
            edge.setRoutingId(routingId);
            edge.setFromOperationId(fromOpId);
            edge.setToOperationId(toOpId);
            edge.setFromName(fromName);
            edge.setToName(toName);
            edge.setEdgeType(edgeType);
            routingEdgeRepository.save(edge);

            log.debug("[storeEdges] ✓ EDGE STORED: {} -> {} ({})", fromName, toName, edgeType);
        }

        log.info("[storeEdges] Successfully stored {} edges", edgesRaw.size());
    }

    private List<ProcessPlanStepRequest> parseAndValidateStrictSteps(List<Map<String, Object>> stepsRaw) {
        if (stepsRaw == null || stepsRaw.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body must be a non-empty JSON array");
        }

        Set<String> allowedKeys = Set.of("name", "description", "sequence", "operation_type", "stage_group", "standard_time");
        List<ProcessPlanStepRequest> steps = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> row : stepsRaw) {
            index++;
            if (row == null || row.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step " + index + " is empty");
            }
            Set<String> keys = new HashSet<>(row.keySet());
            keys.removeAll(allowedKeys);
            if (!keys.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step " + index + " has unsupported fields: " + keys);
            }
            if (!row.containsKey("name")
                    || !row.containsKey("description")
                    || !row.containsKey("sequence")
                    || !row.containsKey("operation_type")
                    || !row.containsKey("stage_group")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Step " + index + " must contain name, description, sequence, operation_type, stage_group");
            }

            ProcessPlanStepRequest step;
            try {
                step = objectMapper.convertValue(row, ProcessPlanStepRequest.class);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step " + index + " has invalid field types");
            }
            validateStep(step, index);
            steps.add(step);
        }
        return steps;
    }

    private void validateStep(ProcessPlanStepRequest step, int index) {
        if (step.getName() == null || step.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step " + index + ": name is required");
        }
        if (step.getDescription() == null || step.getDescription().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step " + index + ": description is required");
        }
        if (step.getSequence() == null || step.getSequence() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step " + index + ": sequence must be > 0");
        }
        if (step.getOperationType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step " + index + ": operation_type is required");
        }
        if (step.getStageGroup() == null || step.getStageGroup() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step " + index + ": stage_group must be > 0");
        }
    }

    private ProcessPlanResponse.OperationResponse toOperationResponse(Operation op) {
        ProcessPlanResponse.OperationResponse opResp = new ProcessPlanResponse.OperationResponse();
        opResp.setOperationId(op.getOperationId());
        opResp.setName(op.getName());
        opResp.setDescription(op.getDescription());
        opResp.setSequence(op.getSequence());
        opResp.setOperationType(op.getOperationType());
        opResp.setStageGroup(op.getStageGroup());
        opResp.setStandardTime(op.getStandardTime());
        return opResp;
    }
}
