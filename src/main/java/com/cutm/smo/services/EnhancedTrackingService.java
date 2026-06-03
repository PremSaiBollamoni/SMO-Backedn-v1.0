package com.cutm.smo.services;

import com.cutm.smo.dto.TrackingRequest;
import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
     * NEW: Supports multi-employee team tracking via request.employeeIds list
     */
    @Transactional
    public Map<String, Object> processTracking(TrackingRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check if multi-employee submission (NEW)
            if (request.getEmployeeIds() != null && !request.getEmployeeIds().isEmpty()) {
                return processMultiEmployeeTracking(request);
            }

            // Single-employee flow (backward compat)
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
                TempActiveAssignment assignment = existingAssignment.get();
                
                // Check if this assignment was recently completed (within last 5 seconds)
                // If so, treat as NEW assignment for next bundle (delete old, create new)
                if ("completed".equalsIgnoreCase(assignment.getStatus())) {
                    // Old completed assignment exists - delete it and create new one for next bundle
                    tempActiveAssignmentRepository.delete(assignment);
                    
                    // Run assignment validations for new bundle
                    TrackingValidationService.ValidationResult assignmentValidation = validationService.runAssignmentValidations(request, empId);
                    if (!assignmentValidation.isValid()) {
                        response.put("success", false);
                        response.put("message", assignmentValidation.getErrorMessage());
                        return response;
                    }
                    
                    return processAssignment(request, empId);
                } else {
                    // Active assignment exists - this is COMPLETION scan
                    return processCompletion(request, assignment, empId);
                }
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
     * NEW: Process multi-employee team tracking
     * Handles submissions with multiple employee IDs in request.employeeIds list
     * Creates separate temp_active_assignment and wiptracking records for each employee
     */
    @Transactional
    private Map<String, Object> processMultiEmployeeTracking(TrackingRequest request) {
        Map<String, Object> response = new HashMap<>();
        List<Long> employeeIds = request.getEmployeeIds();
        
        if (employeeIds == null || employeeIds.isEmpty()) {
            response.put("success", false);
            response.put("message", "No employees provided for team tracking");
            return response;
        }

        System.out.println("[TRACKING] Processing multi-employee team tracking: " + employeeIds.size() + " employees");

        try {
            // Check if any assignment already exists for this machine+tray (team mode detection)
            Optional<TempActiveAssignment> existingTeamAssignment = findTeamAssignment(
                request.getMachineQr(), 
                request.getTrayQr()
            );

            if (existingTeamAssignment.isPresent()) {
                TempActiveAssignment teamAssignment = existingTeamAssignment.get();
                
                // Check if this assignment was recently completed - if so, start new bundle
                if ("completed".equalsIgnoreCase(teamAssignment.getStatus())) {
                    // Old completed assignment exists - delete it and create new one for next bundle
                    tempActiveAssignmentRepository.delete(teamAssignment);
                    return processMultiEmployeeAssignment(request, employeeIds);
                } else {
                    // Active assignment exists - this is COMPLETION scan
                    return processMultiEmployeeCompletion(request, employeeIds, teamAssignment);
                }
            } else {
                // TEAM ASSIGNMENT - Create assignment for all employees
                return processMultiEmployeeAssignment(request, employeeIds);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing multi-employee tracking: " + e.getMessage());
            e.printStackTrace();
            return response;
        }
    }

    /**
     * NEW: Multi-employee ASSIGNMENT FLOW
     * Creates temp_active_assignment record with all employee IDs stored in empIds JSON column
     */
    private Map<String, Object> processMultiEmployeeAssignment(TrackingRequest request, List<Long> employeeIds) {
        Map<String, Object> response = new HashMap<>();

        // Create single team assignment record
        TempActiveAssignment teamAssignment = new TempActiveAssignment();
        teamAssignment.setMachineQr(request.getMachineQr());
        teamAssignment.setTrayQr(request.getTrayQr());
        teamAssignment.setEmpId(employeeIds.get(0)); // Store first employee as primary
        teamAssignment.setEmpIds(employeeIdsToJson(employeeIds)); // Store all employee IDs in JSON
        teamAssignment.setAssignedBy(request.getSupervisorId());
        teamAssignment.setStatus("assigned");

        tempActiveAssignmentRepository.save(teamAssignment);

        // Log assignment for each employee
        for (Long empId : employeeIds) {
            logEvent(request, empId, "TEAM_ASSIGN", "Worker assigned to team job");
        }

        System.out.println("[TRACKING] Team assignment created: " + employeeIds.size() + " workers assigned");

        response.put("success", true);
        response.put("flowType", "ASSIGNMENT");
        response.put("message", "Team of " + employeeIds.size() + " workers assigned to Machine & Tray");
        response.put("tempId", teamAssignment.getTempId());
        response.put("machineQr", request.getMachineQr());
        response.put("trayQr", request.getTrayQr());
        response.put("employeeCount", employeeIds.size());
        response.put("startTime", teamAssignment.getStartTime() != null ? teamAssignment.getStartTime().toString() : null);

        return response;
    }

    /**
     * NEW: Multi-employee COMPLETION FLOW
     * Completes job and creates separate wiptracking record for each employee
     * Each employee gets credited with full tray quantity
     */
    private Map<String, Object> processMultiEmployeeCompletion(TrackingRequest request, List<Long> employeeIds, TempActiveAssignment teamAssignment) {
        Map<String, Object> response = new HashMap<>();

        // Get bin for validation and progression
        Optional<Bin> binOpt = binRepository.findByQrCode(teamAssignment.getTrayQr());
        if (!binOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "Bin not found for tray QR: " + teamAssignment.getTrayQr());
            return response;
        }

        Bin bin = binOpt.get();

        // Auto-detect operation from bin if not provided
        Long operationId = request.getOperationId();
        if (operationId == null && bin.getCurrentOperationId() != null) {
            operationId = bin.getCurrentOperationId();
        }

        // Validate operation can be completed
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

        // Update bin status to ACTIVE if new
        if ("new".equalsIgnoreCase(bin.getStatus())) {
            bin.setStatus("ACTIVE");
            binRepository.save(bin);
        }

        // Create separate wiptracking records for EACH employee
        List<WipTracking> wipRecords = new ArrayList<>();
        for (Long empId : employeeIds) {
            WipTracking wipTracking = new WipTracking();
            wipTracking.setOperatorId(empId); // Each record has different operator
            wipTracking.setStartTime(teamAssignment.getStartTime());
            wipTracking.setEndTime(LocalDateTime.now());
            wipTracking.setStatus(request.getStatus());
            wipTracking.setBinId(bin.getBinId());
            if (operationId != null) {
                wipTracking.setOperationId(operationId);
            }
            // Each employee gets credited with full tray quantity
            if (bin.getQty() != null) {
                wipTracking.setQty(bin.getQty());
            }
            wipRecords.add(wipTracking);
        }

        // Save all wiptracking records
        wipTrackingRepository.saveAll(wipRecords);

        System.out.println("[TRACKING] Created " + wipRecords.size() + " wiptracking records (1 per employee)");

        // Mark all previous tracking records for this bin as COMPLETED
        wipTrackingRepository.findAll().stream()
            .filter(w -> w.getBinId() != null && w.getBinId().equals(bin.getBinId()))
            .filter(w -> "PENDING".equalsIgnoreCase(w.getStatus()) || "ASSIGNED".equalsIgnoreCase(w.getStatus()))
            .forEach(w -> {
                w.setStatus("COMPLETED");
                w.setEndTime(LocalDateTime.now());
                wipTrackingRepository.save(w);
            });

        // Log QR event for each employee
        for (Long empId : employeeIds) {
            qrEventService.logQrEvent(
                teamAssignment.getTrayQr(),
                "WIP",
                wipRecords.get(0).getWipId(),
                "TRACKING",
                request.getOperationId(),
                null,
                empId,
                null
            );
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // DISABLED: Do NOT advance bin to next operation
        // Reason: SAM tracking model - operators are fixed at operations, not moving trays
        // Trays stay at same operation for multiple start/stop cycles
        // ═══════════════════════════════════════════════════════════════════════════
        // Advance bin to next operation
        // Map<String, Object> progressionResult = null;
        // if (operationId != null) {
        //     progressionResult = routingProgressionService.advanceToNextOperation(
        //         bin.getBinId(), 
        //         operationId
        //     );
        // }

        // Log completion for each employee
        for (Long empId : employeeIds) {
            logEvent(request, empId, "TEAM_COMPLETE", "Team job completed by worker");
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // Mark temp assignment as completed (keep record for continuous tracking)
        // Workers stay visible at operation until they actually leave
        // ═══════════════════════════════════════════════════════════════════════════
        teamAssignment.setStatus("completed");
        tempActiveAssignmentRepository.save(teamAssignment);

        // Update bin_assignment_history
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
        response.put("machineQr", request.getMachineQr());
        response.put("trayQr", request.getTrayQr());
        response.put("employeeCount", employeeIds.size());
        response.put("wipTrackingCreated", wipRecords.size());

        // Duration tracking
        LocalDateTime startTime = teamAssignment.getStartTime();
        LocalDateTime endTime = LocalDateTime.now();
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

        // Add progression details - DISABLED (trays don't move)
        // if (progressionResult != null) {
        //     boolean workflowComplete = (Boolean) progressionResult.getOrDefault("workflowComplete", false);
        //     
        //     if (workflowComplete) {
        //         response.put("message", "Team Job Completed - Workflow Finished!");
        //         response.put("workflowComplete", true);
        //         response.put("completedAt", progressionResult.get("completedAt"));
        //     } else {
        //         response.put("message", "Team Job Completed & Advanced to Next Operation");
        //         response.put("workflowComplete", false);
        //         response.put("nextOperationId", progressionResult.get("nextOperationId"));
        //     }
        //     
        //     response.put("currentOperationId", progressionResult.get("currentOperationId"));
        //     response.put("binStatus", progressionResult.get("status"));
        // } else {
            response.put("message", "Team Job Completed");
        // }

        System.out.println("[TRACKING] Team completion flow finished: " + employeeIds.size() + " employees credited with full tray quantity");

        return response;
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

        // Update assignment status to completed (keep record for continuous tracking)
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

        // ═══════════════════════════════════════════════════════════════════════════
        // DISABLED: Do NOT advance bin to next operation
        // Reason: SAM tracking model - operators are fixed at operations, not moving trays
        // Trays stay at same operation for multiple start/stop cycles
        // ═══════════════════════════════════════════════════════════════════════════
        // Advance bin to next operation in routing sequence
        // Map<String, Object> progressionResult = null;
        // if (operationId != null) {
        //     progressionResult = routingProgressionService.advanceToNextOperation(
        //         bin.getBinId(), 
        //         operationId
        //     );
        // }

        // Log the completion event
        logEvent(request, empId, "COMPLETE", "Job completed and moved to main tables");

        // ═══════════════════════════════════════════════════════════════════════════
        // KEEP temp assignment active for continuous tracking (SAM model)
        // Don't delete - worker stays visible at operation for next bundle
        // ═══════════════════════════════════════════════════════════════════════════
        // Clean up temp assignment (remove completed record)
        // tempActiveAssignmentRepository.delete(assignment);

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

        // Add progression details - DISABLED (trays don't move)
        // if (progressionResult != null) {
        //     boolean workflowComplete = (Boolean) progressionResult.getOrDefault("workflowComplete", false);
        //     
        //     if (workflowComplete) {
        //         response.put("message", "Job Completed - Workflow Finished! All operations done.");
        //         response.put("workflowComplete", true);
        //         response.put("completedAt", progressionResult.get("completedAt"));
        //     } else {
        //         response.put("message", "Job Completed & Advanced to Next Operation");
        //         response.put("workflowComplete", false);
        //         response.put("nextOperationId", progressionResult.get("nextOperationId"));
        //     }
        //     
        //     response.put("currentOperationId", progressionResult.get("currentOperationId"));
        //     response.put("lastOperationId", progressionResult.get("lastOperationId"));
        //     response.put("binStatus", progressionResult.get("status"));
        // } else {
            response.put("message", "Job Completed");
        // }

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

    // ═════════════════════════════════════════════════════════════════════════════
    // TEAM ASSIGNMENT SUPPORT (NEW) - Handle multiple employees per job
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Check if team assignment exists (multiple employees on same machine+tray)
     * NEW: Support for team assignments with multiple employees
     */
    public Optional<TempActiveAssignment> findTeamAssignment(String machineQr, String trayQr) {
        // Find any assignment for this machine+tray combination (regardless of employee)
        List<TempActiveAssignment> assignments = tempActiveAssignmentRepository.findByMachineQrAndTrayQrAndStatus(machineQr, trayQr, "assigned");
        if (!assignments.isEmpty()) {
            return Optional.of(assignments.get(0));
        }
        assignments = tempActiveAssignmentRepository.findByMachineQrAndTrayQrAndStatus(machineQr, trayQr, "completed");
        return assignments.isEmpty() ? Optional.empty() : Optional.of(assignments.get(0));
    }

    /**
     * Convert comma-separated employee ID string to List<Long>
     * Supports format: "1001,1002,1003" or "[1001,1002,1003]"
     */
    private List<Long> parseEmployeeIds(String empIdsJson) {
        if (empIdsJson == null || empIdsJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            // Parse JSON array format: [1001,1002,1003]
            String cleaned = empIdsJson.trim().replaceAll("^\\[|\\]$", "");
            if (cleaned.isEmpty()) return new ArrayList<>();
            
            List<Long> ids = new ArrayList<>();
            for (String id : cleaned.split(",")) {
                try {
                    ids.add(Long.parseLong(id.trim()));
                } catch (NumberFormatException e) {
                    System.out.println("[WARNING] Failed to parse employee ID: " + id);
                }
            }
            return ids;
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to parse employee IDs: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Convert List<Long> to JSON array string format
     */
    private String employeeIdsToJson(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i));
            if (i < ids.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}