package com.cutm.smo.services;

import com.cutm.smo.dto.WipStatsResponse;
import com.cutm.smo.models.Operation;
import com.cutm.smo.models.WipTracking;
import com.cutm.smo.repositories.OperationRepository;
import com.cutm.smo.repositories.WipTrackingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * WIP Stats Service - Provides real WIP tracking statistics for Monitor WIP screen
 */
@Slf4j
@Service
public class WipStatsService {
    
    private final WipTrackingRepository wipTrackingRepository;
    private final OperationRepository operationRepository;
    
    public WipStatsService(WipTrackingRepository wipTrackingRepository, OperationRepository operationRepository) {
        this.wipTrackingRepository = wipTrackingRepository;
        this.operationRepository = operationRepository;
    }
    
    /**
     * Get comprehensive WIP statistics for Monitor WIP screen
     */
    public WipStatsResponse getWipStats() {
        try {
            log.info("=== GET WIP STATS START ===");
            
            List<WipTracking> allWipRecords = wipTrackingRepository.findAll();
            log.debug("Total WIP records fetched: {}", allWipRecords.size());
            
            WipStatsResponse response = new WipStatsResponse();
            
            // Calculate overall stats
            response.setTotalWipCount(allWipRecords.size());
            response.setActiveWipCount((int) allWipRecords.stream()
                .filter(w -> "ACTIVE".equalsIgnoreCase(w.getStatus()))
                .count());
            response.setPendingWipCount((int) allWipRecords.stream()
                .filter(w -> "PENDING".equalsIgnoreCase(w.getStatus()))
                .count());
            response.setCompletedTodayCount((int) allWipRecords.stream()
                .filter(w -> "COMPLETED".equalsIgnoreCase(w.getStatus()) && isToday(w.getEndTime()))
                .count());
            
            // Status breakdown
            response.setPendingStatus(response.getPendingWipCount());
            response.setActiveStatus(response.getActiveWipCount());
            response.setCompletedStatus((int) allWipRecords.stream()
                .filter(w -> "COMPLETED".equalsIgnoreCase(w.getStatus()))
                .count());
            
            // Operation-wise breakdown
            response.setOperationStats(getOperationStats(allWipRecords));
            
            // Hourly stats for line graph
            response.setHourlyStats(getHourlyStats(allWipRecords));
            
            // Status distribution for pie chart
            response.setStatusDistribution(getStatusDistribution(allWipRecords));
            
            log.info("WIP stats calculated successfully - Total: {}, Active: {}, Pending: {}, Completed Today: {}",
                response.getTotalWipCount(), response.getActiveWipCount(), 
                response.getPendingWipCount(), response.getCompletedTodayCount());
            log.info("=== GET WIP STATS END - SUCCESS ===");
            
            return response;
            
        } catch (Exception e) {
            log.error("Failed to get WIP stats", e);
            throw new RuntimeException("Failed to fetch WIP statistics", e);
        }
    }
    
    /**
     * Get operation-wise WIP statistics
     */
    private List<WipStatsResponse.OperationWipStats> getOperationStats(List<WipTracking> wipRecords) {
        try {
            Map<Long, List<WipTracking>> groupedByOperation = wipRecords.stream()
                .collect(Collectors.groupingBy(WipTracking::getOperationId));
            
            List<WipStatsResponse.OperationWipStats> operationStats = new ArrayList<>();
            
            for (Map.Entry<Long, List<WipTracking>> entry : groupedByOperation.entrySet()) {
                Long operationId = entry.getKey();
                List<WipTracking> records = entry.getValue();
                
                if (operationId == null) continue;
                
                Operation operation = operationRepository.findById(operationId).orElse(null);
                String operationName = operation != null ? operation.getName() : "Operation " + operationId;
                
                int wipCount = (int) records.stream()
                    .filter(w -> !"COMPLETED".equalsIgnoreCase(w.getStatus()))
                    .count();
                int completedCount = (int) records.stream()
                    .filter(w -> "COMPLETED".equalsIgnoreCase(w.getStatus()))
                    .count();
                
                double avgTimeMinutes = records.stream()
                    .filter(w -> w.getStartTime() != null && w.getEndTime() != null)
                    .mapToLong(w -> java.time.temporal.ChronoUnit.MINUTES.between(w.getStartTime(), w.getEndTime()))
                    .average()
                    .orElse(0.0);
                
                operationStats.add(new WipStatsResponse.OperationWipStats(
                    operationId,
                    operationName,
                    wipCount,
                    completedCount,
                    avgTimeMinutes
                ));
            }
            
            return operationStats.stream()
                .sorted(Comparator.comparingInt(WipStatsResponse.OperationWipStats::getWipCount).reversed())
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Failed to calculate operation stats", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get hourly WIP statistics for line graph
     */
    private List<WipStatsResponse.HourlyWipStats> getHourlyStats(List<WipTracking> wipRecords) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<WipStatsResponse.HourlyWipStats> hourlyStats = new ArrayList<>();
            
            // Get stats for last 24 hours
            for (int i = 23; i >= 0; i--) {
                LocalDateTime hourStart = now.minusHours(i).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime hourEnd = hourStart.plusHours(1);
                
                String hour = hourStart.format(DateTimeFormatter.ofPattern("HH:00"));
                
                int completed = (int) wipRecords.stream()
                    .filter(w -> "COMPLETED".equalsIgnoreCase(w.getStatus()))
                    .filter(w -> w.getEndTime() != null && 
                        !w.getEndTime().isBefore(hourStart) && w.getEndTime().isBefore(hourEnd))
                    .count();
                
                int pending = (int) wipRecords.stream()
                    .filter(w -> "PENDING".equalsIgnoreCase(w.getStatus()))
                    .filter(w -> w.getStartTime() != null && 
                        !w.getStartTime().isBefore(hourStart) && w.getStartTime().isBefore(hourEnd))
                    .count();
                
                int active = (int) wipRecords.stream()
                    .filter(w -> "ACTIVE".equalsIgnoreCase(w.getStatus()))
                    .filter(w -> w.getStartTime() != null && 
                        !w.getStartTime().isBefore(hourStart) && w.getStartTime().isBefore(hourEnd))
                    .count();
                
                hourlyStats.add(new WipStatsResponse.HourlyWipStats(hour, completed, pending, active));
            }
            
            return hourlyStats;
            
        } catch (Exception e) {
            log.error("Failed to calculate hourly stats", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get status distribution for pie chart
     */
    private List<WipStatsResponse.StatusDistribution> getStatusDistribution(List<WipTracking> wipRecords) {
        try {
            int total = wipRecords.size();
            if (total == 0) {
                return new ArrayList<>();
            }
            
            List<WipStatsResponse.StatusDistribution> distribution = new ArrayList<>();
            
            String[] statuses = {"PENDING", "ACTIVE", "COMPLETED"};
            for (String status : statuses) {
                int count = (int) wipRecords.stream()
                    .filter(w -> status.equalsIgnoreCase(w.getStatus()))
                    .count();
                
                double percentage = (count * 100.0) / total;
                
                distribution.add(new WipStatsResponse.StatusDistribution(status, count, percentage));
            }
            
            return distribution;
            
        } catch (Exception e) {
            log.error("Failed to calculate status distribution", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Check if a LocalDateTime is today
     */
    private boolean isToday(LocalDateTime dateTime) {
        if (dateTime == null) return false;
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tomorrow = today.plusDays(1);
        return !dateTime.isBefore(today) && dateTime.isBefore(tomorrow);
    }
}
