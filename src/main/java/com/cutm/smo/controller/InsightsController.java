package com.cutm.smo.controller;

import com.cutm.smo.dto.WipStatsResponse;
import com.cutm.smo.services.AccessControlService;
import com.cutm.smo.services.ProcessPlanService;
import com.cutm.smo.services.InsightsService;
import com.cutm.smo.services.WipStatsService;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/insights")
@CrossOrigin(origins = "*")
public class InsightsController {
    
    private final ProcessPlanService processPlanService;
    private final AccessControlService accessControlService;
    private final InsightsService insightsService;
    private final WipStatsService wipStatsService;

    public InsightsController(ProcessPlanService processPlanService, AccessControlService accessControlService, InsightsService insightsService, WipStatsService wipStatsService) {
        this.processPlanService = processPlanService;
        this.accessControlService = accessControlService;
        this.insightsService = insightsService;
        this.wipStatsService = wipStatsService;
    }

    /**
     * Get GM insights - Process Plan statistics
     */
    @GetMapping("/gm")
    public Map<String, Object> getGmInsights(@RequestParam String actorEmpId) {
        try {
            log.info("=== GET GM INSIGHTS START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            
            accessControlService.require(actorEmpId, "PP_APPROVE");
            log.debug("Access control check passed for PP_APPROVE");
            
            // Get process plan statistics
            int pendingCount = processPlanService.getPendingProcessPlans().size();
            int approvedCount = processPlanService.getApprovedProcessPlans().size();
            
            Map<String, Object> insights = new HashMap<>();
            insights.put("pendingProcessPlans", pendingCount);
            insights.put("approvedProcessPlans", approvedCount);
            insights.put("totalProcessPlans", pendingCount + approvedCount);
            insights.put("reportStatus", pendingCount > 0 ? "PENDING_APPROVALS" : "ALL_APPROVED");
            
            // Mock additional data for dashboard
            insights.put("totalWipRecords", 0);
            insights.put("activeWipRecords", 0);
            insights.put("totalInventoryQty", 0);
            
            log.info("GM insights retrieved successfully - Pending: {}, Approved: {}", pendingCount, approvedCount);
            log.info("=== GET GM INSIGHTS END - SUCCESS ===");
            return insights;
            
        } catch (Exception e) {
            log.error("Failed to get GM insights for actor: {}", actorEmpId, e);
            throw e;
        }
    }

    /**
     * Get Supervisor insights - Floor-level statistics with real WIP data
     */
    @GetMapping("/supervisor")
    public Map<String, Object> getSupervisorInsights(@RequestParam(required = false) String actorEmpId) {
        try {
            log.info("=== GET SUPERVISOR INSIGHTS START ===");
            
            // Get real WIP data from wiptracking table
            long totalWipRecords = insightsService.getTotalWipRecords();
            
            // Return real WIP data
            Map<String, Object> insights = new HashMap<>();
            insights.put("activeWipCount", totalWipRecords);
            insights.put("bottleneckOperationCount", 0);
            insights.put("lineBalancingHint", totalWipRecords > 0 ? "Production is active with " + totalWipRecords + " WIP records" : "No active WIP records");
            
            log.info("Supervisor insights retrieved successfully - Total WIP: {}", totalWipRecords);
            log.info("=== GET SUPERVISOR INSIGHTS END - SUCCESS ===");
            return insights;
            
        } catch (Exception e) {
            log.error("Failed to get supervisor insights", e);
            throw e;
        }
    }

    /**
     * Get real WIP statistics with graphs for Monitor WIP screen
     */
    @GetMapping("/supervisor/wip-stats")
    public WipStatsResponse getWipStats(@RequestParam(required = false) String actorEmpId) {
        try {
            log.info("=== GET WIP STATS START ===");
            
            WipStatsResponse stats = wipStatsService.getWipStats();
            
            log.info("WIP stats retrieved successfully");
            log.info("=== GET WIP STATS END - SUCCESS ===");
            return stats;
            
        } catch (Exception e) {
            log.error("Failed to get WIP stats", e);
            throw e;
        }
    }
}

