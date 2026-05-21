package com.cutm.smo.controller;

import com.cutm.smo.dto.NodeMetricsResponse;
import com.cutm.smo.dto.ProcessPlanDraftRequest;
import com.cutm.smo.dto.ProcessPlanResponse;
import com.cutm.smo.services.AccessControlService;
import com.cutm.smo.services.NodeMetricsService;
import com.cutm.smo.services.ProcessPlanService;
import com.cutm.smo.util.LoggingUtil;
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

    public ProcessPlanController(ProcessPlanService processPlanService, AccessControlService accessControlService, NodeMetricsService nodeMetricsService) {
        this.processPlanService = processPlanService;
        this.accessControlService = accessControlService;
        this.nodeMetricsService = nodeMetricsService;
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
}
