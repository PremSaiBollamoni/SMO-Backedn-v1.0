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

        // Auto-generate edges from routing steps
        log.debug("[approveProcessPlan] Auto-generating edges from routing steps");
        autoGenerateEdges(routingId);

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

    /**
     * Auto-generate edges from routing steps when process plan is approved
     * ONLY if no explicit edges were already provided by the frontend
     */
    @Transactional
    private void autoGenerateEdges(Long routingId) {
        log.info("[autoGenerateEdges] === START AUTO-GENERATING EDGES FOR ROUTING {} ===", routingId);
        
        // Check if edges already exist (frontend provided explicit edges)
        List<RoutingEdge> existingEdges = routingEdgeRepository.findByRoutingIdOrderByEdgeIdAsc(routingId);
        if (!existingEdges.isEmpty()) {
            log.info("[autoGenerateEdges] ✓ EDGES ALREADY EXIST ({} edges) - SKIPPING AUTO-GENERATION", existingEdges.size());
            log.info("[autoGenerateEdges] === END AUTO-GENERATING EDGES (SKIPPED) ===");
            return;
        }
        
        List<RoutingStep> steps = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(routingId);
        if (steps.isEmpty()) {
            log.warn("[autoGenerateEdges] No routing steps found for routing {}", routingId);
            return;
        }

        Set<Long> operationIds = steps.stream().map(RoutingStep::getOperationId).collect(Collectors.toSet());
        Map<Long, Operation> operationById = operationRepository.findAllById(operationIds).stream()
                .collect(Collectors.toMap(Operation::getOperationId, o -> o));

        // Build edges from routing steps (only if no explicit edges exist)
        List<WorkflowEdge> edges = buildExplicitEdges(steps, operationById);
        
        // Store edges in routing_edge table
        Long nextEdgeId = routingEdgeRepository.findMaxEdgeId() + 1;
        for (WorkflowEdge edge : edges) {
            RoutingEdge routingEdge = new RoutingEdge();
            routingEdge.setEdgeId(nextEdgeId++);
            routingEdge.setRoutingId(routingId);
            routingEdge.setFromOperationId(edge.getFromOperationId());
            routingEdge.setToOperationId(edge.getToOperationId());
            routingEdge.setFromName(edge.getFromName());
            routingEdge.setToName(edge.getToName());
            routingEdge.setEdgeType(edge.getEdgeType());
            routingEdgeRepository.save(routingEdge);
            log.debug("[autoGenerateEdges] ✓ EDGE STORED: {} -> {} ({})", 
                    edge.getFromName(), edge.getToName(), edge.getEdgeType());
        }
        
        log.info("[autoGenerateEdges] === AUTO-GENERATED {} EDGES FOR ROUTING {} ===", edges.size(), routingId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EDIT-IN-PLACE FEATURE: Insert / Rename within a single routing
    //  Additive only — does not modify any existing public method behavior.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * When an APPROVED routing is edited, reset it to UNDER_REVIEW so the GM
     * must re-approve before it goes live again.
     * DRAFT and other statuses are left unchanged.
     * This is called at the start of every edit operation.
     */
    private void resetToUnderReviewIfApproved(Long routingId) {
        routingRepository.findById(routingId).ifPresent(routing -> {
            if (STATUS_APPROVED.equalsIgnoreCase(routing.getApprovalStatus()) ||
                STATUS_APPROVED.equalsIgnoreCase(routing.getStatus())) {
                routing.setApprovalStatus(STATUS_UNDER_REVIEW);
                routing.setStatus(STATUS_DRAFT);
                routing.setApprovedBy(null);
                routing.setApprovedAt(null);
                routingRepository.save(routing);
                log.info("[editRouting] Routing {} reset to UNDER_REVIEW for GM re-approval", routingId);
            }
        });
    }

    /**
     * Insert a new operation into a routing's flow. Performs edge surgery so
     * the new operation is properly wired into the graph.
     *
     * Linear case (anchor has exactly one outgoing edge):
     *   AFTER mode:  before -> anchor -> next      becomes  before -> anchor -> NEW -> next
     *   BEFORE mode: prev -> anchor -> after       becomes  prev -> NEW -> anchor -> after
     *
     * Specific edge case (when both afterOperationId and beforeOperationId are provided):
     *   Splits exactly that one edge: A -> B  becomes  A -> NEW -> B
     *
     * @param routingId  target routing
     * @param req        insert request (position, anchor, new op data)
     * @return updated process plan (graph) for the routing
     */
    @Transactional
    public ProcessPlanResponse insertOperationIntoRouting(Long routingId, com.cutm.smo.dto.InsertOperationRequest req) {
        log.info("[insertOperation] === START INSERT OPERATION ROUTING={} ===", routingId);
        if (routingId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "routingId is required");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        Routing routing = routingRepository.findById(routingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Routing not found"));

        String mode = req.getMode() == null ? "SPLIT_EDGE" : req.getMode().trim().toUpperCase();

        if ("ADD_BRANCH".equals(mode)) {
            resetToUnderReviewIfApproved(routingId);
            return addParallelBranch(routingId, req);
        }

        // ── SPLIT_EDGE mode (default / backwards-compatible behavior) ──────────
        Long anchorOpId;
        Long targetFromOp;
        Long targetToOp;

        String position = req.getPosition() == null ? "" : req.getPosition().trim().toUpperCase();
        if (req.getAfterOperationId() != null && req.getBeforeOperationId() != null) {
            // Specific edge mode
            targetFromOp = req.getAfterOperationId();
            targetToOp = req.getBeforeOperationId();
            anchorOpId = targetFromOp;
        } else if ("AFTER".equals(position) && req.getAfterOperationId() != null) {
            anchorOpId = req.getAfterOperationId();
            // Find unique outgoing edge from anchor
            List<RoutingEdge> outgoing = routingEdgeRepository.findByRoutingIdAndFromOperationId(routingId, anchorOpId);
            if (outgoing.isEmpty()) {
                // anchor is a terminal node — no edge to split, just append a new edge anchor -> NEW
                targetFromOp = anchorOpId;
                targetToOp = null;
            } else if (outgoing.size() > 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Anchor operation has multiple outgoing edges. Specify both afterOperationId and beforeOperationId to choose a specific edge.");
            } else {
                targetFromOp = anchorOpId;
                targetToOp = outgoing.get(0).getToOperationId();
            }
        } else if ("BEFORE".equals(position) && req.getBeforeOperationId() != null) {
            anchorOpId = req.getBeforeOperationId();
            // Find unique incoming edge by scanning all edges in routing
            List<RoutingEdge> allEdges = routingEdgeRepository.findByRoutingIdOrderByEdgeIdAsc(routingId);
            List<RoutingEdge> incoming = allEdges.stream()
                    .filter(e -> anchorOpId.equals(e.getToOperationId()))
                    .collect(Collectors.toList());
            if (incoming.isEmpty()) {
                // anchor is a root node — no incoming edge, just prepend a new edge NEW -> anchor
                targetFromOp = null;
                targetToOp = anchorOpId;
            } else if (incoming.size() > 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Anchor operation has multiple incoming edges. Specify both afterOperationId and beforeOperationId to choose a specific edge.");
            } else {
                targetFromOp = incoming.get(0).getFromOperationId();
                targetToOp = anchorOpId;
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide either (position=AFTER + afterOperationId), (position=BEFORE + beforeOperationId), or both afterOperationId and beforeOperationId.");
        }

        // Verify routing contains the anchor operation
        List<RoutingStep> existingSteps = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(routingId);
        boolean anchorInRouting = existingSteps.stream().anyMatch(s -> anchorOpId.equals(s.getOperationId()));
        if (!anchorInRouting) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Anchor operation is not part of the selected routing");
        }

        // Reset to UNDER_REVIEW so GM must re-approve after this edit
        resetToUnderReviewIfApproved(routingId);

        // Resolve / create the new operation
        Operation newOperation = resolveOrCreateOperation(req, existingSteps);

        // Insert routing step linking new operation to the routing
        RoutingStep newStep = new RoutingStep();
        newStep.setRoutingStepId(routingStepRepository.findMaxRoutingStepId() + 1);
        newStep.setRoutingId(routingId);
        newStep.setOperationId(newOperation.getOperationId());
        newStep.setStageGroup(newOperation.getStageGroup() == null ? 1 : newOperation.getStageGroup());
        routingStepRepository.save(newStep);

        // Edge surgery
        Long nextEdgeId = routingEdgeRepository.findMaxEdgeId() + 1;

        if (targetFromOp != null && targetToOp != null) {
            // Split the edge from -> to into  from -> NEW -> to
            // Find existing edge to remove
            List<RoutingEdge> outgoing = routingEdgeRepository.findByRoutingIdAndFromOperationId(routingId, targetFromOp);
            RoutingEdge edgeToRemove = null;
            for (RoutingEdge e : outgoing) {
                if (targetToOp.equals(e.getToOperationId())) {
                    edgeToRemove = e;
                    break;
                }
            }
            String preservedType = edgeToRemove == null ? "sequential" : edgeToRemove.getEdgeType();
            if (edgeToRemove != null) {
                routingEdgeRepository.delete(edgeToRemove);
            }

            Operation fromOp = operationRepository.findById(targetFromOp).orElse(null);
            Operation toOp = operationRepository.findById(targetToOp).orElse(null);

            RoutingEdge edge1 = new RoutingEdge();
            edge1.setEdgeId(nextEdgeId++);
            edge1.setRoutingId(routingId);
            edge1.setFromOperationId(targetFromOp);
            edge1.setToOperationId(newOperation.getOperationId());
            edge1.setFromName(fromOp == null ? "" : fromOp.getName());
            edge1.setToName(newOperation.getName());
            edge1.setEdgeType(preservedType);
            routingEdgeRepository.save(edge1);

            RoutingEdge edge2 = new RoutingEdge();
            edge2.setEdgeId(nextEdgeId);
            edge2.setRoutingId(routingId);
            edge2.setFromOperationId(newOperation.getOperationId());
            edge2.setToOperationId(targetToOp);
            edge2.setFromName(newOperation.getName());
            edge2.setToName(toOp == null ? "" : toOp.getName());
            edge2.setEdgeType(preservedType);
            routingEdgeRepository.save(edge2);

            log.info("[insertOperation] ✓ SPLIT EDGE: {} -> {} into {} -> {} -> {}",
                    targetFromOp, targetToOp, targetFromOp, newOperation.getOperationId(), targetToOp);
        } else if (targetFromOp != null) {
            // Append: anchor -> NEW
            Operation fromOp = operationRepository.findById(targetFromOp).orElse(null);
            RoutingEdge edge = new RoutingEdge();
            edge.setEdgeId(nextEdgeId);
            edge.setRoutingId(routingId);
            edge.setFromOperationId(targetFromOp);
            edge.setToOperationId(newOperation.getOperationId());
            edge.setFromName(fromOp == null ? "" : fromOp.getName());
            edge.setToName(newOperation.getName());
            edge.setEdgeType("sequential");
            routingEdgeRepository.save(edge);
            log.info("[insertOperation] ✓ APPENDED: {} -> {}", targetFromOp, newOperation.getOperationId());
        } else if (targetToOp != null) {
            // Prepend: NEW -> anchor
            Operation toOp = operationRepository.findById(targetToOp).orElse(null);
            RoutingEdge edge = new RoutingEdge();
            edge.setEdgeId(nextEdgeId);
            edge.setRoutingId(routingId);
            edge.setFromOperationId(newOperation.getOperationId());
            edge.setToOperationId(targetToOp);
            edge.setFromName(newOperation.getName());
            edge.setToName(toOp == null ? "" : toOp.getName());
            edge.setEdgeType("sequential");
            routingEdgeRepository.save(edge);
            log.info("[insertOperation] ✓ PREPENDED: {} -> {}", newOperation.getOperationId(), targetToOp);
        }

        log.info("[insertOperation] === END INSERT OPERATION ===");
        return getProcessPlan(routingId);
    }

    /**
     * ADD_BRANCH mode: add the new operation as a new parallel branch from
     * the anchor. No existing edges are removed. Optionally connects the new
     * operation to a merge target.
     */
    @Transactional
    private ProcessPlanResponse addParallelBranch(Long routingId, com.cutm.smo.dto.InsertOperationRequest req) {
        log.info("[insertOperation/ADD_BRANCH] routing={}, anchor={}, mergeTarget={}",
                routingId, req.getAfterOperationId(), req.getMergeTargetOperationId());

        if (req.getAfterOperationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "afterOperationId is required when mode=ADD_BRANCH (it's the source of the new branch)");
        }
        Long anchorId = req.getAfterOperationId();

        // Confirm anchor is in the routing
        List<RoutingStep> existingSteps = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(routingId);
        boolean anchorInRouting = existingSteps.stream().anyMatch(s -> anchorId.equals(s.getOperationId()));
        if (!anchorInRouting) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Anchor operation is not part of the selected routing");
        }

        // Confirm merge target (if provided) is in the routing
        Long mergeTargetId = req.getMergeTargetOperationId();
        if (mergeTargetId != null) {
            boolean targetInRouting = existingSteps.stream().anyMatch(s -> mergeTargetId.equals(s.getOperationId()));
            if (!targetInRouting) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Merge target is not part of the selected routing");
            }
            if (mergeTargetId.equals(anchorId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Merge target cannot be the same as the anchor");
            }
        }

        // Resolve / create the new operation (same logic as split-edge mode)
        Operation newOperation = resolveOrCreateOperation(req, existingSteps);

        // Add routing step linking new op to this routing
        RoutingStep newStep = new RoutingStep();
        newStep.setRoutingStepId(routingStepRepository.findMaxRoutingStepId() + 1);
        newStep.setRoutingId(routingId);
        newStep.setOperationId(newOperation.getOperationId());
        newStep.setStageGroup(newOperation.getStageGroup() == null ? 1 : newOperation.getStageGroup());
        routingStepRepository.save(newStep);

        // Add edge anchor -> NEW (additive, never removes existing edges)
        Long nextEdgeId = routingEdgeRepository.findMaxEdgeId() + 1;
        Operation anchorOp = operationRepository.findById(anchorId).orElse(null);
        RoutingEdge edge1 = new RoutingEdge();
        edge1.setEdgeId(nextEdgeId++);
        edge1.setRoutingId(routingId);
        edge1.setFromOperationId(anchorId);
        edge1.setToOperationId(newOperation.getOperationId());
        edge1.setFromName(anchorOp == null ? "" : anchorOp.getName());
        edge1.setToName(newOperation.getName());
        edge1.setEdgeType("parallel");
        routingEdgeRepository.save(edge1);
        log.info("[insertOperation/ADD_BRANCH] ✓ NEW BRANCH EDGE: {} -> {}", anchorId, newOperation.getOperationId());

        // Optionally connect NEW -> mergeTarget
        if (mergeTargetId != null) {
            Operation mergeOp = operationRepository.findById(mergeTargetId).orElse(null);
            RoutingEdge edge2 = new RoutingEdge();
            edge2.setEdgeId(nextEdgeId);
            edge2.setRoutingId(routingId);
            edge2.setFromOperationId(newOperation.getOperationId());
            edge2.setToOperationId(mergeTargetId);
            edge2.setFromName(newOperation.getName());
            edge2.setToName(mergeOp == null ? "" : mergeOp.getName());
            edge2.setEdgeType("parallel");
            routingEdgeRepository.save(edge2);
            log.info("[insertOperation/ADD_BRANCH] ✓ MERGE EDGE: {} -> {}", newOperation.getOperationId(), mergeTargetId);
        }

        log.info("[insertOperation/ADD_BRANCH] === END ===");
        return getProcessPlan(routingId);
    }

    /**
     * Shared helper used by both SPLIT_EDGE and ADD_BRANCH paths.
     * Either looks up an existing operation or creates a new one.
     */
    private Operation resolveOrCreateOperation(com.cutm.smo.dto.InsertOperationRequest req, List<RoutingStep> existingSteps) {
        if (req.isUseExisting()) {
            if (req.getExistingOperationId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "existingOperationId is required when useExisting=true");
            }
            Operation op = operationRepository.findById(req.getExistingOperationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Existing operation not found"));
            final Long checkOpId = op.getOperationId();
            boolean alreadyInRouting = existingSteps.stream().anyMatch(s -> checkOpId.equals(s.getOperationId()));
            if (alreadyInRouting) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This operation is already part of the routing");
            }
            return op;
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required when creating a new operation");
        }
        Operation newOp = new Operation();
        newOp.setOperationId(operationRepository.findMaxOperationId() + 1);
        newOp.setName(req.getName().trim());
        newOp.setDescription(req.getDescription() == null ? "" : req.getDescription().trim());
        newOp.setSequence(req.getSequence() == null ? 0 : req.getSequence());
        try {
            newOp.setOperationType(req.getOperationType() == null
                    ? OperationType.SEQUENTIAL
                    : OperationType.valueOf(req.getOperationType().trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            newOp.setOperationType(OperationType.SEQUENTIAL);
        }
        newOp.setStageGroup(req.getStageGroup() == null ? 1 : req.getStageGroup());
        newOp.setStandardTime(req.getStandardTime() == null ? 0 : req.getStandardTime());
        return operationRepository.save(newOp);
    }

    /**
     * Rename an operation within a SPECIFIC routing only.
     *
     * Strategy: if the operation is shared with any other routing's steps,
     * clone the operation first (new operation_id), update this routing's
     * step + edges to point at the clone, and rename the clone. This way the
     * rename never bleeds into other routings.
     *
     * @return updated process plan (graph) for the routing
     */
    @Transactional
    public ProcessPlanResponse renameOperationInRouting(Long routingId, com.cutm.smo.dto.RenameOperationRequest req) {
        log.info("[renameOperation] === START RENAME ROUTING={} OP={} ===", routingId, req == null ? null : req.getOperationId());
        if (routingId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "routingId is required");
        }
        if (req == null || req.getOperationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "operationId is required");
        }
        if (req.getNewName() == null || req.getNewName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "newName is required");
        }

        routingRepository.findById(routingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Routing not found"));

        Operation operation = operationRepository.findById(req.getOperationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operation not found"));

        // Verify this operation is referenced by the selected routing
        List<RoutingStep> stepsInRouting = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(routingId);
        RoutingStep targetStep = stepsInRouting.stream()
                .filter(s -> req.getOperationId().equals(s.getOperationId()))
                .findFirst()
                .orElse(null);
        if (targetStep == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Operation is not part of the selected routing");
        }

        // Reset to UNDER_REVIEW so GM must re-approve after this edit
        resetToUnderReviewIfApproved(routingId);

        // Detect if shared with other routings
        List<RoutingStep> stepsAcrossAllRoutings = routingStepRepository.findAll();
        boolean sharedWithOthers = stepsAcrossAllRoutings.stream()
                .anyMatch(s -> req.getOperationId().equals(s.getOperationId())
                        && !routingId.equals(s.getRoutingId()));

        Operation toRename;
        if (sharedWithOthers) {
            // Clone operation, then redirect this routing's step + edges to clone
            Operation clone = new Operation();
            clone.setOperationId(operationRepository.findMaxOperationId() + 1);
            clone.setName(operation.getName());
            clone.setDescription(operation.getDescription());
            clone.setSequence(operation.getSequence());
            clone.setOperationType(operation.getOperationType());
            clone.setStageGroup(operation.getStageGroup());
            clone.setStandardTime(operation.getStandardTime());
            clone = operationRepository.save(clone);

            // Repoint routing step to clone
            targetStep.setOperationId(clone.getOperationId());
            routingStepRepository.save(targetStep);

            // Repoint any edges referencing the original op within this routing only
            List<RoutingEdge> edges = routingEdgeRepository.findByRoutingIdOrderByEdgeIdAsc(routingId);
            for (RoutingEdge e : edges) {
                boolean changed = false;
                if (req.getOperationId().equals(e.getFromOperationId())) {
                    e.setFromOperationId(clone.getOperationId());
                    changed = true;
                }
                if (req.getOperationId().equals(e.getToOperationId())) {
                    e.setToOperationId(clone.getOperationId());
                    changed = true;
                }
                if (changed) {
                    routingEdgeRepository.save(e);
                }
            }

            toRename = clone;
            log.info("[renameOperation] Cloned shared operation {} -> {} for routing {}",
                    operation.getOperationId(), clone.getOperationId(), routingId);
        } else {
            toRename = operation;
        }

        // Apply rename
        toRename.setName(req.getNewName().trim());
        if (req.getNewDescription() != null) {
            toRename.setDescription(req.getNewDescription().trim());
        }
        operationRepository.save(toRename);

        // Sync cached names in routing_edge for this routing
        List<RoutingEdge> edgesAfter = routingEdgeRepository.findByRoutingIdOrderByEdgeIdAsc(routingId);
        for (RoutingEdge e : edgesAfter) {
            boolean changed = false;
            if (toRename.getOperationId().equals(e.getFromOperationId())
                    && !toRename.getName().equals(e.getFromName())) {
                e.setFromName(toRename.getName());
                changed = true;
            }
            if (toRename.getOperationId().equals(e.getToOperationId())
                    && !toRename.getName().equals(e.getToName())) {
                e.setToName(toRename.getName());
                changed = true;
            }
            if (changed) {
                routingEdgeRepository.save(e);
            }
        }

        log.info("[renameOperation] === END RENAME ===");
        return getProcessPlan(routingId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DELETE / RECONNECT / MOVE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Remove an operation from a routing's flow and (by default) auto-bridge
     * its predecessors to its successors so the flow stays connected.
     *
     * Behavior:
     *   - Removes the routing step linking this operation to the routing.
     *   - Removes all edges in this routing that touch the operation.
     *   - If autoBridge=true (default), creates fresh edges from each
     *     predecessor to each successor (cartesian product, deduped).
     *   - The operation itself is NOT deleted from the operation table —
     *     other routings may still reference it.
     */
    @Transactional
    public ProcessPlanResponse removeOperationFromRouting(Long routingId, Long operationId, boolean autoBridge) {
        log.info("[deleteOperation] === START routingId={}, opId={}, autoBridge={} ===",
                routingId, operationId, autoBridge);

        if (routingId == null || operationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "routingId and operationId are required");
        }

        routingRepository.findById(routingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Routing not found"));

        // Verify operation belongs to this routing
        List<RoutingStep> stepsInRouting = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(routingId);
        RoutingStep step = stepsInRouting.stream()
                .filter(s -> operationId.equals(s.getOperationId()))
                .findFirst()
                .orElse(null);
        if (step == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operation is not part of the selected routing");
        }

        // Reset to UNDER_REVIEW so GM must re-approve after this edit
        resetToUnderReviewIfApproved(routingId);

        // Find all edges in this routing
        List<RoutingEdge> allEdges = routingEdgeRepository.findByRoutingIdOrderByEdgeIdAsc(routingId);
        List<Long> predecessors = new ArrayList<>();
        List<Long> successors = new ArrayList<>();
        List<RoutingEdge> toRemove = new ArrayList<>();

        for (RoutingEdge e : allEdges) {
            boolean touches = false;
            if (operationId.equals(e.getToOperationId())) {
                predecessors.add(e.getFromOperationId());
                touches = true;
            }
            if (operationId.equals(e.getFromOperationId())) {
                successors.add(e.getToOperationId());
                touches = true;
            }
            if (touches) {
                toRemove.add(e);
            }
        }

        // Remove all edges touching this op
        for (RoutingEdge e : toRemove) {
            routingEdgeRepository.delete(e);
        }
        log.info("[deleteOperation] removed {} edges, predecessors={}, successors={}",
                toRemove.size(), predecessors, successors);

        // Auto-bridge predecessors -> successors
        if (autoBridge && !predecessors.isEmpty() && !successors.isEmpty()) {
            Long nextEdgeId = routingEdgeRepository.findMaxEdgeId() + 1;
            // Operation lookup map for names
            Map<Long, Operation> opMap = operationRepository.findAllById(
                    java.util.stream.Stream.concat(predecessors.stream(), successors.stream())
                            .distinct().collect(Collectors.toList())).stream()
                    .collect(Collectors.toMap(Operation::getOperationId, o -> o));

            // Existing edges set after deletes (rebuild)
            Set<String> existingPairs = routingEdgeRepository.findByRoutingIdOrderByEdgeIdAsc(routingId).stream()
                    .map(e -> e.getFromOperationId() + "->" + e.getToOperationId())
                    .collect(Collectors.toSet());

            for (Long p : predecessors) {
                for (Long s : successors) {
                    if (p.equals(s)) continue; // would be self-loop
                    String key = p + "->" + s;
                    if (existingPairs.contains(key)) continue;
                    RoutingEdge bridge = new RoutingEdge();
                    bridge.setEdgeId(nextEdgeId++);
                    bridge.setRoutingId(routingId);
                    bridge.setFromOperationId(p);
                    bridge.setToOperationId(s);
                    bridge.setFromName(opMap.get(p) != null ? opMap.get(p).getName() : "");
                    bridge.setToName(opMap.get(s) != null ? opMap.get(s).getName() : "");
                    bridge.setEdgeType("sequential");
                    routingEdgeRepository.save(bridge);
                    existingPairs.add(key);
                    log.info("[deleteOperation] ✓ BRIDGE EDGE: {} -> {}", p, s);
                }
            }
        }

        // Remove the routing step
        routingStepRepository.delete(step);

        log.info("[deleteOperation] === END ===");
        return getProcessPlan(routingId);
    }

    /**
     * Redirect an existing edge to a new target operation in the same routing.
     * Removes the old edge and creates a new one with the same edge_type.
     */
    @Transactional
    public ProcessPlanResponse reconnectEdge(Long routingId, com.cutm.smo.dto.ReconnectEdgeRequest req) {
        log.info("[reconnectEdge] === START routingId={}, from={}, oldTo={}, newTo={} ===",
                routingId, req == null ? null : req.getFromOperationId(),
                req == null ? null : req.getOldToOperationId(),
                req == null ? null : req.getNewToOperationId());

        if (req == null || req.getFromOperationId() == null || req.getOldToOperationId() == null
                || req.getNewToOperationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "fromOperationId, oldToOperationId and newToOperationId are required");
        }
        if (req.getOldToOperationId().equals(req.getNewToOperationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New target is the same as old target");
        }
        if (req.getFromOperationId().equals(req.getNewToOperationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot connect a node to itself");
        }

        routingRepository.findById(routingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Routing not found"));

        // Verify both operations are in the routing
        List<RoutingStep> stepsInRouting = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(routingId);
        Set<Long> opIdsInRouting = stepsInRouting.stream().map(RoutingStep::getOperationId).collect(Collectors.toSet());
        if (!opIdsInRouting.contains(req.getFromOperationId()) ||
            !opIdsInRouting.contains(req.getNewToOperationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Both source and new target must be operations in this routing");
        }

        // Reset to UNDER_REVIEW so GM must re-approve after this edit
        resetToUnderReviewIfApproved(routingId);

        // Find the existing edge from -> oldTo
        List<RoutingEdge> outgoing = routingEdgeRepository.findByRoutingIdAndFromOperationId(routingId, req.getFromOperationId());
        RoutingEdge existing = null;
        for (RoutingEdge e : outgoing) {
            if (req.getOldToOperationId().equals(e.getToOperationId())) {
                existing = e;
                break;
            }
        }
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Edge from -> oldTo does not exist");
        }

        // Make sure new edge wouldn't be a duplicate
        for (RoutingEdge e : outgoing) {
            if (req.getNewToOperationId().equals(e.getToOperationId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "An edge from this source to the new target already exists");
            }
        }

        String preservedType = existing.getEdgeType() == null ? "sequential" : existing.getEdgeType();
        routingEdgeRepository.delete(existing);

        Operation fromOp = operationRepository.findById(req.getFromOperationId()).orElse(null);
        Operation newToOp = operationRepository.findById(req.getNewToOperationId()).orElse(null);

        Long nextEdgeId = routingEdgeRepository.findMaxEdgeId() + 1;
        RoutingEdge fresh = new RoutingEdge();
        fresh.setEdgeId(nextEdgeId);
        fresh.setRoutingId(routingId);
        fresh.setFromOperationId(req.getFromOperationId());
        fresh.setToOperationId(req.getNewToOperationId());
        fresh.setFromName(fromOp == null ? "" : fromOp.getName());
        fresh.setToName(newToOp == null ? "" : newToOp.getName());
        fresh.setEdgeType(preservedType);
        routingEdgeRepository.save(fresh);

        log.info("[reconnectEdge] === END ===");
        return getProcessPlan(routingId);
    }

    /**
     * Move an existing operation to a new position in the routing.
     * Strategy: detach (auto-bridge old neighbors), then add new edges
     * directly based on chosen mode. The routing step row stays intact —
     * only edges change.
     */
    @Transactional
    public ProcessPlanResponse moveOperation(Long routingId, com.cutm.smo.dto.MoveOperationRequest req) {
        log.info("[moveOperation] === START routingId={}, opId={}, mode={}, position={}, anchor={} ===",
                routingId,
                req == null ? null : req.getOperationId(),
                req == null ? null : req.getMode(),
                req == null ? null : req.getPosition(),
                req == null ? null : req.getAnchorOperationId());

        if (req == null || req.getOperationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "operationId is required");
        }
        Long opId = req.getOperationId();

        routingRepository.findById(routingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Routing not found"));

        List<RoutingStep> stepsInRouting = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(routingId);
        boolean inRouting = stepsInRouting.stream().anyMatch(s -> opId.equals(s.getOperationId()));
        if (!inRouting) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operation is not part of the selected routing");
        }

        // Reset to UNDER_REVIEW so GM must re-approve after this edit
        resetToUnderReviewIfApproved(routingId);

        String mode = req.getMode() == null ? "SPLIT_EDGE" : req.getMode().trim().toUpperCase();

        // Step 1: Detach (remove all edges touching this op, optionally bridging)
        detachOperationFromRouting(routingId, opId, !req.isSkipAutoBridge());

        if ("TERMINAL".equals(mode)) {
            log.info("[moveOperation] === END (TERMINAL) ===");
            return getProcessPlan(routingId);
        }

        if (req.getAnchorOperationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "anchorOperationId is required for non-TERMINAL moves");
        }
        Long anchorId = req.getAnchorOperationId();
        if (anchorId.equals(opId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move an operation relative to itself");
        }
        boolean anchorInRouting = stepsInRouting.stream().anyMatch(s -> anchorId.equals(s.getOperationId()));
        if (!anchorInRouting) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Anchor operation is not part of the selected routing");
        }

        Operation anchorOp = operationRepository.findById(anchorId).orElse(null);
        Operation movingOp = operationRepository.findById(opId).orElse(null);
        Long nextEdgeId = routingEdgeRepository.findMaxEdgeId() + 1;

        // Step 2: Add new edges based on mode
        if ("ADD_BRANCH".equals(mode)) {
            // anchor -> moving op
            RoutingEdge e1 = new RoutingEdge();
            e1.setEdgeId(nextEdgeId++);
            e1.setRoutingId(routingId);
            e1.setFromOperationId(anchorId);
            e1.setToOperationId(opId);
            e1.setFromName(anchorOp == null ? "" : anchorOp.getName());
            e1.setToName(movingOp == null ? "" : movingOp.getName());
            e1.setEdgeType("parallel");
            routingEdgeRepository.save(e1);

            if (req.getMergeTargetOperationId() != null) {
                Long mt = req.getMergeTargetOperationId();
                boolean mtInRouting = stepsInRouting.stream().anyMatch(s -> mt.equals(s.getOperationId()));
                if (!mtInRouting) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch end operation is not part of the selected routing");
                }
                Operation mtOp = operationRepository.findById(mt).orElse(null);
                RoutingEdge e2 = new RoutingEdge();
                e2.setEdgeId(nextEdgeId);
                e2.setRoutingId(routingId);
                e2.setFromOperationId(opId);
                e2.setToOperationId(mt);
                e2.setFromName(movingOp == null ? "" : movingOp.getName());
                e2.setToName(mtOp == null ? "" : mtOp.getName());
                e2.setEdgeType("parallel");
                routingEdgeRepository.save(e2);
            }
            log.info("[moveOperation] === END (ADD_BRANCH) ===");
            return getProcessPlan(routingId);
        }

        // SPLIT_EDGE
        String position = req.getPosition() == null ? "" : req.getPosition().trim().toUpperCase();
        Long otherEnd = req.getOtherEndOperationId();

        if ("AFTER".equals(position)) {
            // Determine other end if not provided (must be unique)
            if (otherEnd == null) {
                List<RoutingEdge> outgoing = routingEdgeRepository.findByRoutingIdAndFromOperationId(routingId, anchorId);
                if (outgoing.size() > 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Anchor has multiple outgoing edges. Specify otherEndOperationId.");
                }
                otherEnd = outgoing.isEmpty() ? null : outgoing.get(0).getToOperationId();
            }
            insertBetween(routingId, anchorId, otherEnd, opId, nextEdgeId);
        } else if ("BEFORE".equals(position)) {
            if (otherEnd == null) {
                List<RoutingEdge> all = routingEdgeRepository.findByRoutingIdOrderByEdgeIdAsc(routingId);
                final Long anchorRef = anchorId;
                List<RoutingEdge> incoming = all.stream()
                        .filter(e -> anchorRef.equals(e.getToOperationId()))
                        .collect(Collectors.toList());
                if (incoming.size() > 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Anchor has multiple incoming edges. Specify otherEndOperationId.");
                }
                otherEnd = incoming.isEmpty() ? null : incoming.get(0).getFromOperationId();
            }
            insertBetween(routingId, otherEnd, anchorId, opId, nextEdgeId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "position must be AFTER or BEFORE for SPLIT_EDGE");
        }

        log.info("[moveOperation] === END (SPLIT_EDGE) ===");
        return getProcessPlan(routingId);
    }

    /**
     * Helper: insert moving operation between fromOp and toOp.
     * If fromOp is null → just create toOp's predecessor: opId -> toOp (prepend)
     * If toOp is null → just create fromOp's successor: fromOp -> opId (append)
     * Otherwise: split fromOp -> toOp into fromOp -> opId -> toOp.
     */
    private void insertBetween(Long routingId, Long fromOp, Long toOp, Long opId, Long startingEdgeId) {
        Long nextEdgeId = startingEdgeId;
        Operation movingOp = operationRepository.findById(opId).orElse(null);
        Operation fromOpEntity = fromOp == null ? null : operationRepository.findById(fromOp).orElse(null);
        Operation toOpEntity = toOp == null ? null : operationRepository.findById(toOp).orElse(null);
        String movingName = movingOp == null ? "" : movingOp.getName();

        if (fromOp != null && toOp != null) {
            // Remove existing edge fromOp -> toOp
            List<RoutingEdge> outgoing = routingEdgeRepository.findByRoutingIdAndFromOperationId(routingId, fromOp);
            for (RoutingEdge e : outgoing) {
                if (toOp.equals(e.getToOperationId())) {
                    routingEdgeRepository.delete(e);
                    break;
                }
            }

            RoutingEdge e1 = new RoutingEdge();
            e1.setEdgeId(nextEdgeId++);
            e1.setRoutingId(routingId);
            e1.setFromOperationId(fromOp);
            e1.setToOperationId(opId);
            e1.setFromName(fromOpEntity == null ? "" : fromOpEntity.getName());
            e1.setToName(movingName);
            e1.setEdgeType("sequential");
            routingEdgeRepository.save(e1);

            RoutingEdge e2 = new RoutingEdge();
            e2.setEdgeId(nextEdgeId);
            e2.setRoutingId(routingId);
            e2.setFromOperationId(opId);
            e2.setToOperationId(toOp);
            e2.setFromName(movingName);
            e2.setToName(toOpEntity == null ? "" : toOpEntity.getName());
            e2.setEdgeType("sequential");
            routingEdgeRepository.save(e2);
        } else if (fromOp != null) {
            RoutingEdge e1 = new RoutingEdge();
            e1.setEdgeId(nextEdgeId);
            e1.setRoutingId(routingId);
            e1.setFromOperationId(fromOp);
            e1.setToOperationId(opId);
            e1.setFromName(fromOpEntity == null ? "" : fromOpEntity.getName());
            e1.setToName(movingName);
            e1.setEdgeType("sequential");
            routingEdgeRepository.save(e1);
        } else if (toOp != null) {
            RoutingEdge e1 = new RoutingEdge();
            e1.setEdgeId(nextEdgeId);
            e1.setRoutingId(routingId);
            e1.setFromOperationId(opId);
            e1.setToOperationId(toOp);
            e1.setFromName(movingName);
            e1.setToName(toOpEntity == null ? "" : toOpEntity.getName());
            e1.setEdgeType("sequential");
            routingEdgeRepository.save(e1);
        }
    }

    /**
     * Internal helper: remove all edges touching operationId in routingId,
     * optionally bridging predecessors -> successors.
     * Does NOT remove the routing step itself, so the operation remains in the routing
     * (becomes a terminal/orphan node when called by TERMINAL mode).
     */
    private void detachOperationFromRouting(Long routingId, Long operationId, boolean autoBridge) {
        List<RoutingEdge> allEdges = routingEdgeRepository.findByRoutingIdOrderByEdgeIdAsc(routingId);
        List<Long> predecessors = new ArrayList<>();
        List<Long> successors = new ArrayList<>();
        List<RoutingEdge> toRemove = new ArrayList<>();
        for (RoutingEdge e : allEdges) {
            boolean touches = false;
            if (operationId.equals(e.getToOperationId())) {
                predecessors.add(e.getFromOperationId());
                touches = true;
            }
            if (operationId.equals(e.getFromOperationId())) {
                successors.add(e.getToOperationId());
                touches = true;
            }
            if (touches) toRemove.add(e);
        }
        for (RoutingEdge e : toRemove) {
            routingEdgeRepository.delete(e);
        }
        if (autoBridge && !predecessors.isEmpty() && !successors.isEmpty()) {
            Long nextEdgeId = routingEdgeRepository.findMaxEdgeId() + 1;
            Map<Long, Operation> opMap = operationRepository.findAllById(
                    java.util.stream.Stream.concat(predecessors.stream(), successors.stream())
                            .distinct().collect(Collectors.toList())).stream()
                    .collect(Collectors.toMap(Operation::getOperationId, o -> o));
            Set<String> existingPairs = routingEdgeRepository.findByRoutingIdOrderByEdgeIdAsc(routingId).stream()
                    .map(e -> e.getFromOperationId() + "->" + e.getToOperationId())
                    .collect(Collectors.toSet());
            for (Long p : predecessors) {
                for (Long s : successors) {
                    if (p.equals(s)) continue;
                    String key = p + "->" + s;
                    if (existingPairs.contains(key)) continue;
                    RoutingEdge bridge = new RoutingEdge();
                    bridge.setEdgeId(nextEdgeId++);
                    bridge.setRoutingId(routingId);
                    bridge.setFromOperationId(p);
                    bridge.setToOperationId(s);
                    bridge.setFromName(opMap.get(p) != null ? opMap.get(p).getName() : "");
                    bridge.setToName(opMap.get(s) != null ? opMap.get(s).getName() : "");
                    bridge.setEdgeType("sequential");
                    routingEdgeRepository.save(bridge);
                    existingPairs.add(key);
                }
            }
        }
    }

    /**
     * Add a new edge from one existing operation to another in the same routing.
     * Both operations must already be in the routing. Duplicate edges are rejected.
     */
    @Transactional
    public ProcessPlanResponse addEdgeBetweenOperations(Long routingId, com.cutm.smo.dto.AddEdgeRequest req) {
        log.info("[addEdge] routingId={}, from={}, to={}",
                routingId,
                req == null ? null : req.getFromOperationId(),
                req == null ? null : req.getToOperationId());

        if (req == null || req.getFromOperationId() == null || req.getToOperationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromOperationId and toOperationId are required");
        }
        if (req.getFromOperationId().equals(req.getToOperationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot connect a step to itself");
        }

        routingRepository.findById(routingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Routing not found"));

        List<RoutingStep> stepsInRouting = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(routingId);
        Set<Long> opIdsInRouting = stepsInRouting.stream().map(RoutingStep::getOperationId).collect(Collectors.toSet());
        if (!opIdsInRouting.contains(req.getFromOperationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source step is not part of the selected routing");
        }
        if (!opIdsInRouting.contains(req.getToOperationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target step is not part of the selected routing");
        }

        // Reset to UNDER_REVIEW so GM must re-approve after this edit
        resetToUnderReviewIfApproved(routingId);

        // Check for duplicate edge
        List<RoutingEdge> outgoing = routingEdgeRepository.findByRoutingIdAndFromOperationId(routingId, req.getFromOperationId());
        for (RoutingEdge e : outgoing) {
            if (req.getToOperationId().equals(e.getToOperationId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A connection already exists between these two steps");
            }
        }

        Operation fromOp = operationRepository.findById(req.getFromOperationId()).orElse(null);
        Operation toOp = operationRepository.findById(req.getToOperationId()).orElse(null);
        String edgeType = (req.getEdgeType() == null || req.getEdgeType().trim().isEmpty())
                ? "sequential" : req.getEdgeType().trim();

        RoutingEdge edge = new RoutingEdge();
        edge.setEdgeId(routingEdgeRepository.findMaxEdgeId() + 1);
        edge.setRoutingId(routingId);
        edge.setFromOperationId(req.getFromOperationId());
        edge.setToOperationId(req.getToOperationId());
        edge.setFromName(fromOp == null ? "" : fromOp.getName());
        edge.setToName(toOp == null ? "" : toOp.getName());
        edge.setEdgeType(edgeType);
        routingEdgeRepository.save(edge);

        log.info("[addEdge] ✓ EDGE ADDED: {} -> {}", req.getFromOperationId(), req.getToOperationId());
        return getProcessPlan(routingId);
    }
}
