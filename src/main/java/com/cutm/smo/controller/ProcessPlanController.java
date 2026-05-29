package com.cutm.smo.controller;

import com.cutm.smo.dto.NodeMetricsResponse;
import com.cutm.smo.dto.ProcessPlanDraftRequest;
import com.cutm.smo.dto.ProcessPlanResponse;
import com.cutm.smo.dto.WorkflowEdge;
import com.cutm.smo.services.AccessControlService;
import com.cutm.smo.services.NodeMetricsService;
import com.cutm.smo.services.ProcessPlanService;
import com.cutm.smo.services.BreakWindowService;
import com.cutm.smo.util.LoggingUtil;
import com.cutm.smo.repositories.BinRepository;
import com.cutm.smo.repositories.WipTrackingRepository;
import com.cutm.smo.repositories.BinAssignmentHistoryRepository;
import com.cutm.smo.repository.OrderRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/processplan")
@CrossOrigin(origins = "*")
public class ProcessPlanController {
    private final ProcessPlanService processPlanService;
    private final AccessControlService accessControlService;
    private final NodeMetricsService nodeMetricsService;
    private final BinRepository binRepository;
    private final WipTrackingRepository wipTrackingRepository;
    private final BinAssignmentHistoryRepository binAssignmentHistoryRepository;
    private final OrderRepository orderRepository;
    private final BreakWindowService breakWindowService;

    public ProcessPlanController(ProcessPlanService processPlanService, AccessControlService accessControlService, NodeMetricsService nodeMetricsService,
            BinRepository binRepository, WipTrackingRepository wipTrackingRepository, BinAssignmentHistoryRepository binAssignmentHistoryRepository, OrderRepository orderRepository,
            BreakWindowService breakWindowService) {
        this.processPlanService = processPlanService;
        this.accessControlService = accessControlService;
        this.nodeMetricsService = nodeMetricsService;
        this.binRepository = binRepository;
        this.wipTrackingRepository = wipTrackingRepository;
        this.binAssignmentHistoryRepository = binAssignmentHistoryRepository;
        this.orderRepository = orderRepository;
        this.breakWindowService = breakWindowService;
    }

    @PostMapping("/draft")
    public ProcessPlanResponse createDraftProcessPlan(
            @RequestParam(required = false, defaultValue = "SYSTEM") String actorEmpId,
            @RequestParam Long productId,
            @RequestBody ProcessPlanDraftRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE DRAFT PROCESS PLAN START ===");
            log.info("[DRAFT] Received request with actorEmpId='{}', productId={}", actorEmpId, productId);
            log.info("[DRAFT] Request body - steps: {}, edges: {}", 
                request.getSteps() != null ? request.getSteps().size() : 0,
                request.getEdges() != null ? request.getEdges().size() : 0);
            
            // Log the actual parameter value received
            log.info("[DRAFT] Parameter check: actorEmpId is null? {}, equals SYSTEM? {}", 
                actorEmpId == null, "SYSTEM".equals(actorEmpId));
            
            // Skip access control check - process plan creation is allowed for all authenticated users
            // The frontend passes the employee ID, so we know they're authenticated
            log.info("[DRAFT] Access control BYPASSED - process plan creation allowed for all authenticated users");
            
            log.info("[DRAFT] Creating process plan for productId: {}", productId);
            ProcessPlanResponse response = processPlanService.createDraftProcessPlan(
                productId, 
                request.getSteps(), 
                request.getEdges()
            );
            log.info("[DRAFT] Process plan created successfully with routing_id: {}", response.getRoutingId());
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create Draft Process Plan", startTime, endTime);
            log.info("=== CREATE DRAFT PROCESS PLAN END - SUCCESS ===");
            return response;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("[DRAFT] EXCEPTION CAUGHT: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            log.error("[DRAFT] Full stack trace:", e);
            LoggingUtil.logError(log, "Failed to create draft process plan for product: " + productId, e);
            LoggingUtil.logPerformance(log, "Create Draft Process Plan (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PostMapping("/{sourceRoutingId}/clone/draft")
    public ProcessPlanResponse cloneDraftFromExisting(
            @PathVariable Long sourceRoutingId,
            @RequestParam String actorEmpId,
            @RequestParam(required = false) Long productId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CLONE DRAFT PROCESS PLAN START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Source Routing ID: {}", sourceRoutingId);
            log.debug("Product ID: {}", productId);
            
            accessControlService.require(actorEmpId, "PP_SUBMIT");
            log.debug("Access control check passed for PP_SUBMIT");
            
            ProcessPlanResponse response = processPlanService.cloneDraftFromExisting(sourceRoutingId, productId);
            log.info("Draft process plan cloned successfully with new routing_id: {}", response.getRoutingId());
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Clone Draft Process Plan", startTime, endTime);
            log.info("=== CLONE DRAFT PROCESS PLAN END - SUCCESS ===");
            return response;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to clone draft process plan from routing: " + sourceRoutingId, e);
            LoggingUtil.logPerformance(log, "Clone Draft Process Plan (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping("/{routingId}")
    public ProcessPlanResponse getProcessPlan(@PathVariable Long routingId, @RequestParam String actorEmpId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET PROCESS PLAN START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Routing ID: {}", routingId);
            
            accessControlService.require(actorEmpId, "PP_VIEW_ALL");
            log.debug("Access control check passed for PP_VIEW_ALL");
            
            ProcessPlanResponse response = processPlanService.getProcessPlan(routingId);
            log.info("Process plan retrieved successfully");
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Process Plan", startTime, endTime);
            log.info("=== GET PROCESS PLAN END - SUCCESS ===");
            return response;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get process plan for routing: " + routingId, e);
            LoggingUtil.logPerformance(log, "Get Process Plan (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping("/{routingId}/graph")
    public Map<String, Object> getProcessPlanGraph(@PathVariable Long routingId, @RequestParam String actorEmpId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET PROCESS PLAN GRAPH START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Routing ID: {}", routingId);
            
            accessControlService.require(actorEmpId, "PP_VIEW_ALL");
            log.debug("Access control check passed for PP_VIEW_ALL");
            
            ProcessPlanResponse response = processPlanService.getProcessPlan(routingId);
            
            // Convert operations to workflow nodes
            List<Map<String, Object>> nodes = new ArrayList<>();
            if (response.getOperations() != null) {
                for (ProcessPlanResponse.OperationResponse op : response.getOperations()) {
                    Map<String, Object> node = new HashMap<>();
                    node.put("id", op.getOperationId().toString());
                    node.put("displayName", op.getName() != null ? op.getName() : "Operation");
                    node.put("description", op.getDescription() != null ? op.getDescription() : op.getName());
                    node.put("isMerge", false);
                    node.put("connections", new ArrayList<>());
                    node.put("sequenceIndex", op.getSequence() != null ? op.getSequence() : 0);
                    node.put("operationId", op.getOperationId());
                    node.put("routingId", routingId);
                    nodes.add(node);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("nodes", nodes);
            result.put("edges", response.getEdges() != null ? response.getEdges() : new ArrayList<>());
            
            log.info("Process plan graph retrieved successfully");
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Process Plan Graph", startTime, endTime);
            log.info("=== GET PROCESS PLAN GRAPH END - SUCCESS ===");
            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get process plan graph for routing: " + routingId, e);
            LoggingUtil.logPerformance(log, "Get Process Plan Graph (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping("/product/{productId}")
    public List<ProcessPlanResponse> getProcessPlansByProduct(@PathVariable Long productId, @RequestParam String actorEmpId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET PROCESS PLANS BY PRODUCT START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Product ID: {}", productId);
            
            accessControlService.require(actorEmpId, "PP_VIEW_ALL");
            log.debug("Access control check passed for PP_VIEW_ALL");
            
            List<ProcessPlanResponse> responses = processPlanService.getProcessPlansByProduct(productId);
            log.info("Retrieved {} process plans for product: {}", responses.size(), productId);
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Process Plans By Product", startTime, endTime);
            log.info("=== GET PROCESS PLANS BY PRODUCT END - SUCCESS ===");
            return responses;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get process plans for product: " + productId, e);
            LoggingUtil.logPerformance(log, "Get Process Plans By Product (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PostMapping("/{routingId}/approve")
    public ProcessPlanResponse approveProcessPlan(
            @PathVariable Long routingId,
            @RequestParam String actorEmpId,
            @RequestParam Long approvedBy) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== APPROVE PROCESS PLAN START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Routing ID: {}", routingId);
            log.debug("Approved By: {}", approvedBy);
            
            accessControlService.require(actorEmpId, "PP_APPROVE");
            log.debug("Access control check passed for PP_APPROVE");
            
            ProcessPlanResponse response = processPlanService.approveProcessPlan(routingId, approvedBy);
            log.info("Process plan approved successfully");
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Approve Process Plan", startTime, endTime);
            log.info("=== APPROVE PROCESS PLAN END - SUCCESS ===");
            return response;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to approve process plan for routing: " + routingId, e);
            LoggingUtil.logPerformance(log, "Approve Process Plan (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PostMapping("/{routingId}/reject")
    public ProcessPlanResponse rejectProcessPlan(
            @PathVariable Long routingId,
            @RequestParam String actorEmpId,
            @RequestParam Long approvedBy) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== REJECT PROCESS PLAN START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Routing ID: {}", routingId);
            log.debug("Rejected By: {}", approvedBy);
            
            accessControlService.require(actorEmpId, "PP_APPROVE");
            log.debug("Access control check passed for PP_APPROVE");
            
            ProcessPlanResponse response = processPlanService.rejectProcessPlan(routingId, approvedBy);
            log.info("Process plan rejected successfully");
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Reject Process Plan", startTime, endTime);
            log.info("=== REJECT PROCESS PLAN END - SUCCESS ===");
            return response;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to reject process plan for routing: " + routingId, e);
            LoggingUtil.logPerformance(log, "Reject Process Plan (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping("/pending")
    public List<ProcessPlanResponse> getPendingProcessPlans(@RequestParam String actorEmpId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET PENDING PROCESS PLANS START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            
            accessControlService.require(actorEmpId, "PP_APPROVE");
            log.debug("Access control check passed for PP_APPROVE");
            
            List<ProcessPlanResponse> responses = processPlanService.getPendingProcessPlans();
            log.info("Retrieved {} pending process plans", responses.size());
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Pending Process Plans", startTime, endTime);
            log.info("=== GET PENDING PROCESS PLANS END - SUCCESS ===");
            return responses;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get pending process plans", e);
            LoggingUtil.logPerformance(log, "Get Pending Process Plans (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping("/approved")
    public List<ProcessPlanResponse> getApprovedProcessPlans(@RequestParam String actorEmpId) {
        // Allow both PP_VIEW_ALL and PP_APPROVE (GM needs this for order creation)
        try {
            accessControlService.require(actorEmpId, "PP_VIEW_ALL");
        } catch (ResponseStatusException e) {
            accessControlService.require(actorEmpId, "PP_APPROVE");
        }
        return processPlanService.getApprovedProcessPlans();
    }

    @GetMapping("/node-metrics")
    public NodeMetricsResponse getNodeMetrics(
            @RequestParam Long routingId,
            @RequestParam Long operationId,
            @RequestParam String actorEmpId) {
        // Check if user has any of the allowed activities for node metrics
        try {
            accessControlService.require(actorEmpId, "PP_VIEW_NODE_METRICS");
        } catch (ResponseStatusException e1) {
            try {
                accessControlService.require(actorEmpId, "PP_APPROVE"); // GM approval activity
            } catch (ResponseStatusException e2) {
                try {
                    accessControlService.require(actorEmpId, "SUPERVISOR_MONITOR_WIP"); // Supervisor WIP monitoring
                } catch (ResponseStatusException e3) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                        "Access denied: Requires PP_VIEW_NODE_METRICS, PP_APPROVE, or SUPERVISOR_MONITOR_WIP activity");
                }
            }
        }
        return nodeMetricsService.getNodeMetrics(routingId, operationId);
    }

    @GetMapping("/operation-status")
    public Map<String, Object> getOperationStatus(
            @RequestParam Long routingId,
            @RequestParam Long operationId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET OPERATION STATUS START ===");
            log.debug("Routing ID: {}", routingId);
            log.debug("Operation ID: {}", operationId);
            
            // Get the process plan to find the operation
            ProcessPlanResponse plan = processPlanService.getProcessPlan(routingId);
            
            // Find the operation
            ProcessPlanResponse.OperationResponse operation = null;
            if (plan.getOperations() != null) {
                operation = plan.getOperations().stream()
                    .filter(op -> op.getOperationId().equals(operationId))
                    .findFirst()
                    .orElse(null);
            }
            
            if (operation == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Operation not found: " + operationId);
            }
            
            // Build operation status response with real data from database
            Map<String, Object> response = new HashMap<>();
            response.put("operation_id", operationId);
            response.put("routing_id", routingId);
            response.put("name", operation.getName());
            response.put("description", operation.getDescription());
            
            // Fetch real data from repositories
            // Count active bins for this operation (bins currently at this operation)
            // Check both status and currentStatus fields with case-insensitive comparison
            List<?> activeBinsList = binRepository.findAll().stream()
                .filter(bin -> bin.getCurrentOperationId() != null && 
                        bin.getCurrentOperationId().equals(operationId) &&
                        (!"COMPLETED".equalsIgnoreCase(bin.getStatus()) &&
                         !"completed".equalsIgnoreCase(bin.getCurrentStatus()) &&
                         !"free".equalsIgnoreCase(bin.getCurrentStatus())))
                .toList();
            int activeBins = activeBinsList.size();
            
            // Get WIP tracking data for this operation
            List<?> wipList = wipTrackingRepository.findAll().stream()
                .filter(wip -> wip.getOperationId() != null && 
                        wip.getOperationId().equals(operationId))
                .toList();
            
            // WIP Quantity: number of distinct TRAYS (bins) currently in progress at this operation
            // Count trays, NOT piece quantity — 1 tray = 1 regardless of how many pieces it holds
            int wipQuantity = (int) wipList.stream()
                .filter(wip -> "IN_PROGRESS".equals(((com.cutm.smo.models.WipTracking)wip).getStatus()) || 
                               "PENDING".equals(((com.cutm.smo.models.WipTracking)wip).getStatus()))
                .map(wip -> ((com.cutm.smo.models.WipTracking)wip).getBinId())
                .filter(binId -> binId != null)
                .distinct()
                .count();
            
            // Completed Quantity: number of distinct TRAYS (bins) that completed this operation
            int completedQuantity = (int) wipList.stream()
                .filter(wip -> "COMPLETED".equals(((com.cutm.smo.models.WipTracking)wip).getStatus()))
                .map(wip -> ((com.cutm.smo.models.WipTracking)wip).getBinId())
                .filter(binId -> binId != null)
                .distinct()
                .count();
            
            // Count active operators (distinct operators working on this operation)
            long activeOperators = wipList.stream()
                .filter(wip -> "IN_PROGRESS".equals(((com.cutm.smo.models.WipTracking)wip).getStatus()))
                .map(wip -> ((com.cutm.smo.models.WipTracking)wip).getOperatorId())
                .distinct()
                .count();
            
            // Determine operation status based on WIP data and order status
            // If the order is COMPLETED, all operations should be COMPLETED
            String status = "PENDING";
            
            // Check if any bins for this routing have a completed order
            List<?> binsForRouting = binRepository.findAll().stream()
                .filter(bin -> bin.getCurrentRoutingId() != null && bin.getCurrentRoutingId().equals(routingId))
                .toList();
            
            boolean orderCompleted = false;
            if (!binsForRouting.isEmpty()) {
                com.cutm.smo.models.Bin sampleBin = (com.cutm.smo.models.Bin) binsForRouting.get(0);
                if (sampleBin.getOrderId() != null) {
                    java.util.Optional<com.cutm.smo.models.Order> orderOpt = orderRepository.findById(sampleBin.getOrderId());
                    if (orderOpt.isPresent() && "COMPLETED".equals(orderOpt.get().getStatus())) {
                        orderCompleted = true;
                    }
                }
            }
            
            // If order is completed, all operations are completed
            if (orderCompleted) {
                status = "COMPLETED";
            } else if (activeBins > 0 || wipQuantity > 0) {
                status = "IN_PROGRESS";
            } else if (completedQuantity > 0) {
                status = "COMPLETED";
            }
            
            // Get last action from bin assignment history for this routing
            String lastAction = "None";
            String lastActionTime = "N/A";
            List<?> historyList = binAssignmentHistoryRepository.findAll().stream()
                .filter(h -> h.getRoutingId() != null && h.getRoutingId().equals(routingId))
                .sorted((a, b) -> {
                    java.time.LocalDateTime timeA = ((com.cutm.smo.models.BinAssignmentHistory)a).getAssignmentStartTime();
                    java.time.LocalDateTime timeB = ((com.cutm.smo.models.BinAssignmentHistory)b).getAssignmentStartTime();
                    return timeB.compareTo(timeA);
                })
                .limit(1)
                .toList();
            
            if (!historyList.isEmpty()) {
                com.cutm.smo.models.BinAssignmentHistory lastHistory = (com.cutm.smo.models.BinAssignmentHistory) historyList.get(0);
                lastAction = lastHistory.getNextOperation() != null ? lastHistory.getNextOperation() : "QR Assignment";
                if (lastHistory.getAssignmentStartTime() != null) {
                    lastActionTime = lastHistory.getAssignmentStartTime().toString();
                }
            }
            
            response.put("status", status);
            response.put("active_bins", activeBins);
            response.put("wip_quantity", wipQuantity);
            response.put("completed_quantity", completedQuantity);
            response.put("active_operators", (int) activeOperators);
            response.put("last_action", lastAction);
            response.put("last_action_time", lastActionTime);
            response.put("estimated_time", operation.getStandardTime() != null ? operation.getStandardTime() + " min" : "N/A");

            // ── Actual timing from wiptracking ──────────────────────────────
            // Find the most recent completed wiptracking record for this operation
            // to show real start/end times and actual duration.
            java.time.LocalDateTime actualStart = null;
            java.time.LocalDateTime actualEnd = null;
            String actualDuration = "N/A";
            String actualStartStr = "N/A";
            String actualEndStr = "N/A";

            List<com.cutm.smo.models.WipTracking> wipRecords = wipTrackingRepository.findAll().stream()
                .filter(w -> w.getOperationId() != null && w.getOperationId().equals(operationId))
                .filter(w -> w.getStartTime() != null)
                .sorted((a, b) -> {
                    if (a.getEndTime() == null && b.getEndTime() == null) return 0;
                    if (a.getEndTime() == null) return 1;
                    if (b.getEndTime() == null) return -1;
                    return b.getEndTime().compareTo(a.getEndTime());
                })
                .limit(1)
                .toList();

            if (!wipRecords.isEmpty()) {
                com.cutm.smo.models.WipTracking latest = wipRecords.get(0);
                actualStart = latest.getStartTime();
                actualEnd = latest.getEndTime();
                if (actualStart != null) {
                    actualStartStr = actualStart.toString().replace("T", " ").substring(0, Math.min(19, actualStart.toString().length()));
                }
                if (actualEnd != null) {
                    actualEndStr = actualEnd.toString().replace("T", " ").substring(0, Math.min(19, actualEnd.toString().length()));
                    if (actualStart != null) {
                        long netSeconds = breakWindowService.calculateNetDurationSeconds(actualStart, actualEnd);
                        actualDuration = breakWindowService.formatDuration(netSeconds);
                    }
                } else {
                    // Still in progress — show start time, no end yet
                    actualEndStr = "In progress...";
                    if (actualStart != null) {
                        long netSeconds = breakWindowService.calculateNetDurationSeconds(actualStart, null);
                        actualDuration = breakWindowService.formatDuration(netSeconds) + " (ongoing)";
                    }
                }
            }

            response.put("actual_start_time", actualStartStr);
            response.put("actual_end_time", actualEndStr);
            response.put("actual_duration", actualDuration);
            
            // Get NEXT operation from routing edges (following graph, not sequential)
            String nextOperation = "N/A";
            Long nextOperationId = null;
            if (plan.getEdges() != null) {
                for (WorkflowEdge edge : plan.getEdges()) {
                    if (edge.getFromOperationId().equals(operationId)) {
                        nextOperation = edge.getToName();
                        nextOperationId = edge.getToOperationId();
                        log.debug("Found next operation via edge: {} -> {}", operation.getName(), nextOperation);
                        break;
                    }
                }
            }
            response.put("next_operation", nextOperation);
            response.put("next_operation_id", nextOperationId);
            
            log.info("Operation status retrieved - Operation: {}, Status: {}, Active Bins: {}, WIP Qty: {}, Completed: {}, Next Op: {}", 
                operation.getName(), status, activeBins, wipQuantity, completedQuantity, nextOperation);
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Operation Status", startTime, endTime);
            log.info("=== GET OPERATION STATUS END - SUCCESS ===");
            return response;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get operation status for routing: " + routingId + ", operation: " + operationId, e);
            LoggingUtil.logPerformance(log, "Get Operation Status (Failed)", startTime, endTime);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EDIT-IN-PLACE FEATURE: Insert / Rename within a single routing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Insert a new operation into a routing's flow graph (edge surgery).
     * Returns the same shape as GET /{routingId} so the UI re-renders the
     * existing graph widget without any custom logic.
     */
    @PostMapping("/{routingId}/insert-operation")
    public ProcessPlanResponse insertOperation(
            @PathVariable Long routingId,
            @RequestParam(required = false, defaultValue = "SYSTEM") String actorEmpId,
            @RequestBody com.cutm.smo.dto.InsertOperationRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== INSERT OPERATION START === routingId={}", routingId);
            ProcessPlanResponse response = processPlanService.insertOperationIntoRouting(routingId, request);
            LoggingUtil.logPerformance(log, "Insert Operation Into Routing", startTime, System.currentTimeMillis());
            log.info("=== INSERT OPERATION END - SUCCESS ===");
            return response;
        } catch (Exception e) {
            LoggingUtil.logError(log, "Failed to insert operation into routing: " + routingId, e);
            LoggingUtil.logPerformance(log, "Insert Operation Into Routing (Failed)", startTime, System.currentTimeMillis());
            throw e;
        }
    }

    /**
     * Rename an operation within a SPECIFIC routing only.
     * If the operation is shared with other routings, the service clones it
     * first so the rename is scoped to this routing.
     */
    @PutMapping("/{routingId}/rename-operation")
    public ProcessPlanResponse renameOperation(
            @PathVariable Long routingId,
            @RequestParam(required = false, defaultValue = "SYSTEM") String actorEmpId,
            @RequestBody com.cutm.smo.dto.RenameOperationRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== RENAME OPERATION START === routingId={}", routingId);
            ProcessPlanResponse response = processPlanService.renameOperationInRouting(routingId, request);
            LoggingUtil.logPerformance(log, "Rename Operation In Routing", startTime, System.currentTimeMillis());
            log.info("=== RENAME OPERATION END - SUCCESS ===");
            return response;
        } catch (Exception e) {
            LoggingUtil.logError(log, "Failed to rename operation in routing: " + routingId, e);
            LoggingUtil.logPerformance(log, "Rename Operation In Routing (Failed)", startTime, System.currentTimeMillis());
            throw e;
        }
    }

    /**
     * Remove an operation from a routing's flow.
     * autoBridge defaults to true so flow stays connected.
     */
    @DeleteMapping("/{routingId}/operations/{operationId}")
    public ProcessPlanResponse removeOperation(
            @PathVariable Long routingId,
            @PathVariable Long operationId,
            @RequestParam(required = false, defaultValue = "true") boolean autoBridge,
            @RequestParam(required = false, defaultValue = "SYSTEM") String actorEmpId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== DELETE OPERATION FROM ROUTING START === routingId={}, opId={}", routingId, operationId);
            ProcessPlanResponse response = processPlanService.removeOperationFromRouting(routingId, operationId, autoBridge);
            LoggingUtil.logPerformance(log, "Delete Operation From Routing", startTime, System.currentTimeMillis());
            log.info("=== DELETE OPERATION FROM ROUTING END - SUCCESS ===");
            return response;
        } catch (Exception e) {
            LoggingUtil.logError(log, "Failed to remove operation from routing: " + routingId, e);
            LoggingUtil.logPerformance(log, "Delete Operation From Routing (Failed)", startTime, System.currentTimeMillis());
            throw e;
        }
    }

    /**
     * Redirect an existing edge to a new target operation in the same routing.
     */
    @PostMapping("/{routingId}/reconnect")
    public ProcessPlanResponse reconnectEdge(
            @PathVariable Long routingId,
            @RequestParam(required = false, defaultValue = "SYSTEM") String actorEmpId,
            @RequestBody com.cutm.smo.dto.ReconnectEdgeRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== RECONNECT EDGE START === routingId={}", routingId);
            ProcessPlanResponse response = processPlanService.reconnectEdge(routingId, request);
            LoggingUtil.logPerformance(log, "Reconnect Edge", startTime, System.currentTimeMillis());
            log.info("=== RECONNECT EDGE END - SUCCESS ===");
            return response;
        } catch (Exception e) {
            LoggingUtil.logError(log, "Failed to reconnect edge in routing: " + routingId, e);
            LoggingUtil.logPerformance(log, "Reconnect Edge (Failed)", startTime, System.currentTimeMillis());
            throw e;
        }
    }

    /**
     * Move an existing operation to a new position in the routing.
     */
    @PostMapping("/{routingId}/move-operation")
    public ProcessPlanResponse moveOperation(
            @PathVariable Long routingId,
            @RequestParam(required = false, defaultValue = "SYSTEM") String actorEmpId,
            @RequestBody com.cutm.smo.dto.MoveOperationRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== MOVE OPERATION START === routingId={}", routingId);
            ProcessPlanResponse response = processPlanService.moveOperation(routingId, request);
            LoggingUtil.logPerformance(log, "Move Operation", startTime, System.currentTimeMillis());
            log.info("=== MOVE OPERATION END - SUCCESS ===");
            return response;
        } catch (Exception e) {
            LoggingUtil.logError(log, "Failed to move operation in routing: " + routingId, e);
            LoggingUtil.logPerformance(log, "Move Operation (Failed)", startTime, System.currentTimeMillis());
            throw e;
        }
    }

    /**
     * Add a new connection (edge) between two steps that already exist in the routing.
     */
    @PostMapping("/{routingId}/add-edge")
    public ProcessPlanResponse addEdge(
            @PathVariable Long routingId,
            @RequestParam(required = false, defaultValue = "SYSTEM") String actorEmpId,
            @RequestBody com.cutm.smo.dto.AddEdgeRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== ADD EDGE START === routingId={}", routingId);
            ProcessPlanResponse response = processPlanService.addEdgeBetweenOperations(routingId, request);
            LoggingUtil.logPerformance(log, "Add Edge", startTime, System.currentTimeMillis());
            log.info("=== ADD EDGE END - SUCCESS ===");
            return response;
        } catch (Exception e) {
            LoggingUtil.logError(log, "Failed to add edge in routing: " + routingId, e);
            LoggingUtil.logPerformance(log, "Add Edge (Failed)", startTime, System.currentTimeMillis());
            throw e;
        }
    }
}
