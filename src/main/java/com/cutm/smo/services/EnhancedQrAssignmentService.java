package com.cutm.smo.services;

import com.cutm.smo.dto.QrAssignmentRequest;
import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class EnhancedQrAssignmentService {

    @Autowired
    private BinRepository binRepository;

    @Autowired
    private BinAssignmentHistoryRepository binAssignmentHistoryRepository;

    @Autowired
    private RoutingRepository routingRepository;

    @Autowired
    private StyleVariantRepository styleVariantRepository;

    @Autowired
    private com.cutm.smo.repository.OrderRepository orderRepository;

    @Autowired
    private RoutingProgressionService routingProgressionService;

    @Autowired
    private QrEventService qrEventService;

    /**
     * Enhanced QR assignment with transaction-based workflow
     * Following the flowchart: Transaction → QR Check → Status Validation → Assignment
     */
    @Transactional
    public Map<String, Object> processEnhancedQrAssignment(QrAssignmentRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Step 1: Basic validation
            Map<String, Object> validationResult = validateBasicRequirements(request);
            if (!(Boolean) validationResult.get("success")) {
                return validationResult;
            }

            // Step 2: Check if QR exists in bin table
            Optional<Bin> existingBinOpt = binRepository.findByQrCode(request.getQrCode());
            Bin bin;
            
            if (!existingBinOpt.isPresent()) {
                // Create new bin record
                bin = createNewBinRecord(request);
            } else {
                bin = existingBinOpt.get();
            }

            // Step 3: Check current status - must be 'free'
            Map<String, Object> statusResult = validateBinStatus(bin);
            if (!(Boolean) statusResult.get("success")) {
                return statusResult;
            }

            // Step 4: Validate process plan data
            Map<String, Object> processValidationResult = validateProcessPlanData(request);
            if (!(Boolean) processValidationResult.get("success")) {
                return processValidationResult;
            }

            // Step 5: Execute assignment transaction
            return executeAssignmentTransaction(request, bin, processValidationResult);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing QR assignment: " + e.getMessage());
            response.put("errorType", "SYSTEM_ERROR");
            return response;
        }
    }

    /**
     * Validate basic requirements
     */
    private Map<String, Object> validateBasicRequirements(QrAssignmentRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (request.getProcessPlanNumber() == null || request.getProcessPlanNumber().trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Process Plan Number is required");
            response.put("errorType", "VALIDATION_ERROR");
            return response;
        }

        if (request.getQrCode() == null || request.getQrCode().trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "QR Code is required");
            response.put("errorType", "VALIDATION_ERROR");
            return response;
        }

        if (request.getTrayQuantity() == null || request.getTrayQuantity() <= 0) {
            response.put("success", false);
            response.put("message", "Tray Quantity must be greater than 0");
            response.put("errorType", "VALIDATION_ERROR");
            return response;
        }

        response.put("success", true);
        return response;
    }

    /**
     * Create new bin record if QR doesn't exist
     */
    private Bin createNewBinRecord(QrAssignmentRequest request) {
        Bin bin = new Bin();
        
        // Generate new bin ID
        Long nextBinId = binRepository.getNextBinId();
        bin.setBinId(nextBinId);
        bin.setQrCode(request.getQrCode());
        bin.setStatus("new");
        bin.setCurrentStatus("free");
        bin.setCreatedAt(LocalDateTime.now());
        bin.setQty(0); // Will be updated during assignment
        
        return binRepository.save(bin);
    }

    /**
     * Validate bin status - must be 'free'
     */
    private Map<String, Object> validateBinStatus(Bin bin) {
        Map<String, Object> response = new HashMap<>();

        String currentStatus = bin.getCurrentStatus();
        
        // Check if status is 'free' or null (treat null as free)
        if (currentStatus != null && !"free".equalsIgnoreCase(currentStatus)) {
            response.put("success", false);
            response.put("message", "Tray is already assigned to another job. Please complete current assignment first.");
            response.put("errorType", "STATUS_ERROR");
            response.put("currentStatus", currentStatus);
            return response;
        }

        response.put("success", true);
        return response;
    }

    /**
     * Validate process plan data and fetch related information
     */
    private Map<String, Object> validateProcessPlanData(QrAssignmentRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Parse routing ID from process plan number
            Long routingId = Long.parseLong(request.getProcessPlanNumber());
            
            // Validate routing exists and is approved
            Optional<Routing> routingOpt = routingRepository.findById(routingId);
            if (!routingOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Process Plan not found");
                response.put("errorType", "VALIDATION_ERROR");
                return response;
            }

            Routing routing = routingOpt.get();
            if (!"APPROVED".equalsIgnoreCase(routing.getApprovalStatus())) {
                response.put("success", false);
                response.put("message", "Process Plan is not approved");
                response.put("errorType", "VALIDATION_ERROR");
                return response;
            }

            // Find style variant based on provided data
            Long styleVariantId = findStyleVariantId(request);
            if (styleVariantId == null) {
                response.put("success", false);
                response.put("message", "No matching style variant found for provided data");
                response.put("errorType", "VALIDATION_ERROR");
                return response;
            }

            response.put("success", true);
            response.put("routing", routing);
            response.put("styleVariantId", styleVariantId);
            return response;

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("message", "Invalid Process Plan Number format");
            response.put("errorType", "VALIDATION_ERROR");
            return response;
        }
    }

    /**
     * Find style variant ID based on provided data
     */
    private Long findStyleVariantId(QrAssignmentRequest request) {
        List<StyleVariant> variants = styleVariantRepository.findAll();
        
        for (StyleVariant variant : variants) {
            boolean matches = true;
            
            // Match size if provided
            if (request.getSize() != null && !request.getSize().trim().isEmpty()) {
                if (variant.getSize() == null || !variant.getSize().equalsIgnoreCase(request.getSize())) {
                    matches = false;
                }
            }
            
            // Match GTG number if provided
            if (request.getGtgNumber() != null && !request.getGtgNumber().trim().isEmpty()) {
                if (variant.getGtgId() == null || !variant.getGtgId().equalsIgnoreCase(request.getGtgNumber())) {
                    matches = false;
                }
            }
            
            if (matches) {
                return variant.getStyleVariantId();
            }
        }
        
        return null;
    }

    /**
     * Execute the assignment transaction
     */
    private Map<String, Object> executeAssignmentTransaction(QrAssignmentRequest request, Bin bin, Map<String, Object> processData) {
        Map<String, Object> response = new HashMap<>();

        Routing routing = (Routing) processData.get("routing");
        Long styleVariantId = (Long) processData.get("styleVariantId");
        Long supervisorId = request.getSupervisorId() != null ? request.getSupervisorId() : 1004L; // Default supervisor
        LocalDateTime now = LocalDateTime.now();

        // Step 1: Create assignment history record
        BinAssignmentHistory history = new BinAssignmentHistory();
        history.setBinId(bin.getBinId());
        history.setQrCode(request.getQrCode());
        history.setRoutingId(routing.getRoutingId());
        history.setStyleVariantId(styleVariantId);
        history.setSize(request.getSize());
        history.setGtgId(request.getGtgNumber());
        history.setQty(request.getTrayQuantity());
        history.setAssignmentStartTime(now);
        history.setAssignedBy(supervisorId);
        history.setNextOperation(request.getNextOperation());
        history.setNotes(buildAssignmentNotes(request, routing));
        
        BinAssignmentHistory savedHistory = binAssignmentHistoryRepository.save(history);

        // Step 2: Update bin current state
        bin.setCurrentStatus("assigned");
        bin.setCurrentRoutingId(routing.getRoutingId());
        bin.setCurrentStyleVariantId(styleVariantId);
        bin.setAssignmentStartTime(now);
        bin.setLastAssignedBy(supervisorId);
        bin.setQty(request.getTrayQuantity());
        
        // Link bin to order if orderNumber provided
        if (request.getOrderNumber() != null && !request.getOrderNumber().trim().isEmpty()) {
            Optional<Order> orderOpt = orderRepository.findByOrderNumber(request.getOrderNumber());
            if (orderOpt.isPresent()) {
                bin.setOrderId(orderOpt.get().getOrderId());
            } else {
                // Log warning but don't fail the assignment
                System.out.println("[WARN] Order not found for orderNumber: " + request.getOrderNumber());
            }
        }
        
        Bin savedBin = binRepository.save(bin);

        // Step 3: Initialize routing progression - set current operation to first operation
        Long firstOperationId = routingProgressionService.initializeRoutingProgression(savedBin.getBinId(), routing.getRoutingId());

        // Step 4: Log QR event for audit trail
        qrEventService.logQrEvent(
            request.getQrCode(),
            "BIN",
            savedBin.getBinId(),
            "ASSIGNMENT",
            firstOperationId,
            null,
            supervisorId,
            null
        );

        // Step 5: Build success response
        response.put("success", true);
        response.put("message", "QR Code successfully assigned to Tray. Assignment Start Time recorded");
        response.put("binId", savedBin.getBinId());
        response.put("historyId", savedHistory.getHistoryId());
        response.put("assignmentStartTime", now);
        response.put("assignedBy", supervisorId);
        response.put("routingId", routing.getRoutingId());
        response.put("styleVariantId", styleVariantId);
        response.put("trayQuantity", request.getTrayQuantity());
        response.put("currentOperationId", firstOperationId);
        response.put("firstOperationId", firstOperationId);

        return response;
    }

    /**
     * Build assignment notes for audit trail
     */
    private String buildAssignmentNotes(QrAssignmentRequest request, Routing routing) {
        StringBuilder notes = new StringBuilder();
        notes.append("Enhanced QR Assignment:\n");
        notes.append("Process Plan: ").append(request.getProcessPlanNumber()).append("\n");
        notes.append("QR Code: ").append(request.getQrCode()).append("\n");
        notes.append("Tray Quantity: ").append(request.getTrayQuantity()).append("\n");
        
        if (request.getStyle() != null && !request.getStyle().trim().isEmpty()) {
            notes.append("Style: ").append(request.getStyle()).append("\n");
        }
        if (request.getSize() != null && !request.getSize().trim().isEmpty()) {
            notes.append("Size: ").append(request.getSize()).append("\n");
        }
        if (request.getGtgNumber() != null && !request.getGtgNumber().trim().isEmpty()) {
            notes.append("GTG Number: ").append(request.getGtgNumber()).append("\n");
        }
        if (request.getBtnNumber() != null && !request.getBtnNumber().trim().isEmpty()) {
            notes.append("Button Number: ").append(request.getBtnNumber()).append("\n");
        }
        if (request.getLabel() != null && !request.getLabel().trim().isEmpty()) {
            notes.append("Label: ").append(request.getLabel()).append("\n");
        }
        if (request.getNextOperation() != null && !request.getNextOperation().trim().isEmpty()) {
            notes.append("Next Operation: ").append(request.getNextOperation()).append("\n");
        }
        
        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            notes.append("Additional Notes: ").append(request.getNotes());
        }
        
        return notes.toString();
    }
}