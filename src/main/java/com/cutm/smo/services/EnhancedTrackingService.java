package com.cutm.smo.services;

import com.cutm.smo.dto.TrackingRequest;
import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EnhancedTrackingService {

    @Autowired
    private TempActiveAssignmentRepository tempActiveAssignmentRepository;

    @Autowired
    private TempAssignmentLogRepository tempAssignmentLogRepository;

    @Autowired
    private WipTrackingRepository wipTrackingRepository;

    @Autowired
    private BinRepository binRepository;

    @Autowired
    private TrackingValidationService validationService;

    @Autowired
    private RoutingProgressionService routingProgressionService;

    @Autowired
    private QrEventService qrEventService;

    @Autowired
    private BinAssignmentHistoryRepository binAssignmentHistoryRepository;

    @Autowired
    private BreakWindowService breakWindowService;

    /**
     * Process tracking request with two-phase workflow
     * Automatically detects assignment vs completion based on existing records
     */
    @Transactional
    public Map<String, Object> processTracking(TrackingRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Step 1: Run all validation checks
            TrackingValidationService.ValidationResult validationResult = validationService.runAllValidations(request);
            if (!validationResult.isValid()) {
                response.put("success", false);
                response.put("message", validationResult.getErrorMessage());
                return response;
            }

            Long empId = validationResult.getEmpId();

            // Step 2: Check if combination already exists in temp_active_assignments
            Optional<TempActiveAssignment> existingAssignment = tempActiveAssignmentRepository
                .findByMachineQrAndTrayQrAndEmpId(request.getMachineQr(), request.getTrayQr(), empId);

            if (existingAssignment.isPresent()) {
                // COMPLETION FLOW - Assignment exists, complete it
                return processCompletion(request, existingAssignment.get(), empId);
            } else {
                // ASSIGNMENT FLOW - No existing assignment, create new one
                // Run additional assignment-specific validations
                TrackingValidationService.ValidationResult assignmentValidation = validationService.runAssignmentValidations(request, empId);
                if (!assignmentValidation.isValid()) {
                    response.put("success", false);
                    response.put("message", assignmentValidation.getErrorMessage());
                    return response;
                }
                
                return processAssignment(request, empId);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing tracking: " + e.getMessage());
            return response;
        }
    }

    /**
     * ASSIGNMENT FLOW - Create new assignment
     */
    private Map<String, Object> processAssignment(TrackingRequest request, Long empId) {
        Map<String, Object> response = new HashMap<>();

        // Create new temp assignment
        TempActiveAssignment assignment = new TempActiveAssignment();
        assignment.setMachineQr(request.getMachineQr());
        assignment.setTrayQr(request.getTrayQr());
        assignment.setEmpId(empId);
        assignment.setAssignedBy(request.getSupervisorId());
        assignment.setStatus("assigned");

        tempActiveAssignmentRepository.save(assignment);

        // Log the assignment event
        logEvent(request, empId, "ASSIGN", "Worker assigned to machine and tray");

        response.put("success", true);
        response.put("flowType", "ASSIGNMENT");
        response.put("message", "Worker assigned to Machine & Tray");
        response.put("tempId", assignment.getTempId());
        response.put("machineQr", request.getMachineQr());
        response.put("employeeQr", request.getEmployeeQr());
        response.put("trayQr", request.getTrayQr());
        response.put("startTime", assignment.getStartTime() != null ? assignment.getStartTime().toString() : null);

        return response;
    }

    /**
     * COMPLETION FLOW - Complete existing assignment
     */
    private Map<String, Object> processCompletion(TrackingRequest request, TempActiveAssignment assignment, Long empId) {
        Map<String, Object> response = new HashMap<>();

        // Get bin for validation and progression
        Optional<Bin> binOpt = binRepository.findByQrCode(assignment.getTrayQr());
        if (!binOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "Bin not found for tray QR: " + assignment.getTrayQr());
            return response;
        }

        Bin bin = binOpt.get();

        // Auto-detect operation from bin if not provided
        Long operationId = request.getOperationId();
        if (operationId == null && bin.getCurrentOperationId() != null) {
            operationId = bin.getCurrentOperationId();
        }

        // Validate operation can be completed (sequence check)
        if (operationId != null && bin.getCurrentOperationId() != null) {
            Map<String, Object> validationResult = routingProgressionService.validateOperationCompletion(
                bin.getBinId(), 
                operationId
            );
            
            if (!(Boolean) validationResult.getOrDefault("valid", false)) {
                response.put("success", false);
                response.put("message", validationResult.get("message"));
                response.put("validationError", true);
                return response;
            }
        }

        // ========== FIX: Update bin status to ACTIVE after first operation ==========
        // If bin status is still "new", change it to "ACTIVE" after first tracking
        if ("new".equalsIgnoreCase(bin.getStatus())) {
            bin.setStatus("ACTIVE");
            binRepository.save(bin);
        }

        // Update assignment status to completed
        assignment.setStatus("completed");
        tempActiveAssignmentRepository.save(assignment);

        // Move to main wiptracking table with proper FK population
        WipTracking wipTracking = createWipTrackingRecord(assignment, request, bin);
        wipTrackingRepository.save(wipTracking);

        // IMPORTANT: Mark all previous wiptracking records for this assignment as COMPLETED
        // This ensures that when an operation is completed, all related tracking records are marked as done
        wipTrackingRepository.findAll().stream()
            .filter(w -> w.getBinId() != null && w.getBinId().equals(bin.getBinId()))
            .filter(w -> "PENDING".equalsIgnoreCase(w.getStatus()) || "ASSIGNED".equalsIgnoreCase(w.getStatus()))
            .forEach(w -> {
                w.setStatus("COMPLETED");
                w.setEndTime(LocalDateTime.now());
                wipTrackingRepository.save(w);
            });

        // Log QR event for audit trail
        qrEventService.logQrEvent(
            assignment.getTrayQr(),
            "WIP",
            wipTracking.getWipId(),
            "TRACKING",
            request.getOperationId(),
            null, // machineId not available in current model
            empId,
            null
        );

        // Advance bin to next operation in routing sequence
        Map<String, Object> progressionResult = null;
        if (operationId != null) {
            progressionResult = routingProgressionService.advanceToNextOperation(
                bin.getBinId(), 
                operationId
            );
        }

        // Log the completion event
        logEvent(request, empId, "COMPLETE", "Job completed and moved to main tables");

        // Clean up temp assignment (remove completed record)
        tempActiveAssignmentRepository.delete(assignment);

        // Update bin_assignment_history end time for this bin
        // Find the most recent open history record (no end time) for this bin and close it
        List<BinAssignmentHistory> openHistory = binAssignmentHistoryRepository
                .findByBinIdOrderByCreatedAtDesc(bin.getBinId());
        openHistory.stream()
                .filter(h -> h.getAssignmentEndTime() == null)
                .findFirst()
                .ifPresent(h -> {
                    h.setAssignmentEndTime(LocalDateTime.now());
                    binAssignmentHistoryRepository.save(h);
                });

        // Build response
        response.put("success", true);
        response.put("flowType", "COMPLETION");
        response.put("wipTrackingId", wipTracking.getWipId());
        response.put("machineQr", request.getMachineQr());
        response.put("employeeQr", request.getEmployeeQr());
        response.put("trayQr", request.getTrayQr());

        // Duration tracking - net time between 1st scan (assignment) and 2nd scan (completion)
        // Subtracts any active break windows that fall within the tracking period
        LocalDateTime startTime = wipTracking.getStartTime();
        LocalDateTime endTime = wipTracking.getEndTime();
        if (startTime != null && endTime != null) {
            long netSeconds = breakWindowService.calculateNetDurationSeconds(startTime, endTime);
            long rawSeconds = ChronoUnit.SECONDS.between(startTime, endTime);
            response.put("startTime", startTime.toString());
            response.put("endTime", endTime.toString());
            response.put("durationSeconds", netSeconds);
            response.put("durationFormatted", breakWindowService.formatDuration(netSeconds));
            if (netSeconds < rawSeconds) {
                long breakSeconds = rawSeconds - netSeconds;
                response.put("breakDeductedSeconds", breakSeconds);
                response.put("breakDeductedFormatted", breakWindowService.formatDuration(breakSeconds));
            }
        }

        // Add progression details
        if (progressionResult != null) {
            boolean workflowComplete = (Boolean) progressionResult.getOrDefault("workflowComplete", false);
            
            if (workflowComplete) {
                response.put("message", "Job Completed - Workflow Finished! All operations done.");
                response.put("workflowComplete", true);
                response.put("completedAt", progressionResult.get("completedAt"));
            } else {
                response.put("message", "Job Completed & Advanced to Next Operation");
                response.put("workflowComplete", false);
                response.put("nextOperationId", progressionResult.get("nextOperationId"));
            }
            
            response.put("currentOperationId", progressionResult.get("currentOperationId"));
            response.put("lastOperationId", progressionResult.get("lastOperationId"));
            response.put("binStatus", progressionResult.get("status"));
        } else {
            response.put("message", "Job Completed & moved to main tables");
        }

        return response;
    }

    /**
     * Create WipTracking record from completed assignment with proper FK population
     */
    private WipTracking createWipTrackingRecord(TempActiveAssignment assignment, TrackingRequest request, Bin bin) {
        WipTracking tracking = new WipTracking();
        
        // Don't set wipId - let database auto-generate it
        tracking.setOperatorId(assignment.getEmpId());
        tracking.setStartTime(assignment.getStartTime());
        tracking.setEndTime(LocalDateTime.now());
        tracking.setStatus(request.getStatus());
        
        // FIX: Populate bin_id from resolved bin
        tracking.setBinId(bin.getBinId());
        
        // FIX: Use operation_id from request or auto-detect from bin
        Long operationId = request.getOperationId();
        if (operationId == null && bin.getCurrentOperationId() != null) {
            operationId = bin.getCurrentOperationId();
        }
        if (operationId != null) {
            tracking.setOperationId(operationId);
        }
        
        // Set quantity from bin
        if (bin.getQty() != null) {
            tracking.setQty(bin.getQty());
        }
        
        return tracking;
    }

    /**
     * Log tracking events for audit trail
     */
    private void logEvent(TrackingRequest request, Long empId, String eventType, String notes) {
        TempAssignmentLog log = new TempAssignmentLog();
        log.setMachineQr(request.getMachineQr());
        log.setTrayQr(request.getTrayQr());
        log.setEmpId(empId);
        log.setEventType(eventType);
        log.setSupervisorId(request.getSupervisorId());
        log.setNotes(notes);

        tempAssignmentLogRepository.save(log);
    }

    /**
     * Format duration in seconds into a human-readable string (e.g., "1h 23m 45s")
     */
    private String formatDuration(long totalSeconds) {
        if (totalSeconds < 0) return "0s";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString();
    }
}