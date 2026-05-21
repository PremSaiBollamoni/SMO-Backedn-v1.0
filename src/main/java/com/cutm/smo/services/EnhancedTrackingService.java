package com.cutm.smo.services;

import com.cutm.smo.dto.TrackingRequest;
import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
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

        // Validate operation can be completed (sequence check)
        if (request.getOperationId() != null && bin.getCurrentOperationId() != null) {
            Map<String, Object> validationResult = routingProgressionService.validateOperationCompletion(
                bin.getBinId(), 
                request.getOperationId()
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
        if (request.getOperationId() != null) {
            progressionResult = routingProgressionService.advanceToNextOperation(
                bin.getBinId(), 
                request.getOperationId()
            );
        }

        // Log the completion event
        logEvent(request, empId, "COMPLETE", "Job completed and moved to main tables");

        // Clean up temp assignment (remove completed record)
        tempActiveAssignmentRepository.delete(assignment);

        // Build response
        response.put("success", true);
        response.put("flowType", "COMPLETION");
        response.put("wipTrackingId", wipTracking.getWipId());
        response.put("machineQr", request.getMachineQr());
        response.put("employeeQr", request.getEmployeeQr());
        response.put("trayQr", request.getTrayQr());

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
        
        // Generate new WIP ID
        Long maxId = wipTrackingRepository.findMaxWipTrackingId();
        tracking.setWipId(maxId != null ? maxId + 1 : 1L);
        
        tracking.setOperatorId(assignment.getEmpId());
        tracking.setStartTime(assignment.getStartTime());
        tracking.setEndTime(LocalDateTime.now());
        tracking.setStatus(request.getStatus());
        
        // FIX: Populate bin_id from resolved bin
        tracking.setBinId(bin.getBinId());
        
        // FIX: Populate operation_id from request
        if (request.getOperationId() != null) {
            tracking.setOperationId(request.getOperationId());
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
}