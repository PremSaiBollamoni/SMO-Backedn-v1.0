package com.cutm.smo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * WIP Stats Response DTO - Real WIP tracking data with graphs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WipStatsResponse {
    
    // Overall stats
    private int totalWipCount;
    private int activeWipCount;
    private int completedTodayCount;
    private int pendingWipCount;
    
    // Status breakdown
    private int pendingStatus;
    private int activeStatus;
    private int completedStatus;
    
    // Operation-wise breakdown
    private List<OperationWipStats> operationStats;
    
    // Time-based stats for graphs
    private List<HourlyWipStats> hourlyStats;
    
    // Status distribution for pie chart
    private List<StatusDistribution> statusDistribution;
    
    /**
     * Operation-wise WIP statistics
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationWipStats {
        private Long operationId;
        private String operationName;
        private int wipCount;
        private int completedCount;
        private double avgTimeMinutes;
    }
    
    /**
     * Hourly WIP statistics for line graph
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyWipStats {
        private String hour;
        private int completed;
        private int pending;
        private int active;
    }
    
    /**
     * Status distribution for pie chart
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusDistribution {
        private String status;
        private int count;
        private double percentage;
    }
}
