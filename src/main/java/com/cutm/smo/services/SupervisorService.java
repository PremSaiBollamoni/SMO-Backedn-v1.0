package com.cutm.smo.services;

import com.cutm.smo.dto.QrAssignmentRequest;
import com.cutm.smo.dto.TrackingRequest;
import com.cutm.smo.dto.MergingRequest;
import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SupervisorService {

    @Autowired
    private RoutingRepository routingRepository;

    @Autowired
    private StyleRepository styleRepository;

    @Autowired
    private StyleVariantRepository styleVariantRepository;

    @Autowired
    private ButtonsRepository buttonsRepository;

    @Autowired
    private GarmentRepository garmentRepository;
    
    @Autowired
    private WipTrackingRepository wipTrackingRepository;
    
    @Autowired
    private BinRepository binRepository;

    @Autowired
    private EnhancedTrackingService enhancedTrackingService;

    @Autowired
    private EnhancedMergingService enhancedMergingService;

    @Autowired
    private EnhancedQrAssignmentService enhancedQrAssignmentService;

    @Autowired
    private com.cutm.smo.repositories.RoutingStepRepository routingStepRepository;

    @Autowired
    private com.cutm.smo.repositories.OperationRepository operationRepository;

    /**
     * Get all approved process plan numbers (routing IDs)
     */
    public List<String> getProcessPlans() {
        List<Routing> routings = routingRepository.findAll();
        return routings.stream()
                .filter(r -> "APPROVED".equalsIgnoreCase(r.getApprovalStatus()))
                .map(r -> String.valueOf(r.getRoutingId()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all style numbers
     */
    public List<String> getStyles() {
        List<Style> styles = styleRepository.findAll();
        return styles.stream()
                .filter(s -> s.getStyleNo() != null && !s.getStyleNo().trim().isEmpty())
                .map(Style::getStyleNo)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all sizes from style_variant table
     */
    public List<String> getSizes() {
        List<StyleVariant> variants = styleVariantRepository.findAll();
        return variants.stream()
                .filter(v -> v.getSize() != null && !v.getSize().trim().isEmpty())
                .map(StyleVariant::getSize)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all GTG numbers from style_variant table
     */
    public List<String> getGtgNumbers() {
        List<StyleVariant> variants = styleVariantRepository.findAll();
        return variants.stream()
                .filter(v -> v.getGtgId() != null && !v.getGtgId().trim().isEmpty())
                .map(StyleVariant::getGtgId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all button numbers from buttons table
     */
    public List<String> getBtnNumbers() {
        List<Buttons> buttons = buttonsRepository.findAll();
        return buttons.stream()
                .filter(b -> b.getButtonCode() != null && !b.getButtonCode().trim().isEmpty())
                .map(Buttons::getButtonCode)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all labels from style table (main_label and branding_label)
     */
    public List<String> getLabels() {
        List<Style> styles = styleRepository.findAll();
        Set<String> labels = new HashSet<>();
        
        for (Style style : styles) {
            if (style.getMainLabel() != null && !style.getMainLabel().trim().isEmpty()) {
                labels.add(style.getMainLabel());
            }
            if (style.getBrandingLabel() != null && !style.getBrandingLabel().trim().isEmpty()) {
                labels.add(style.getBrandingLabel());
            }
        }
        
        return labels.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get operations for a specific routing/process plan
     */
    public List<Map<String, Object>> getOperationsForRouting(Long routingId) {
        List<com.cutm.smo.models.RoutingStep> routingSteps = routingStepRepository.findByRoutingId(routingId);
        
        return routingSteps.stream()
                .map(step -> {
                    Map<String, Object> opData = new HashMap<>();
                    opData.put("operationId", step.getOperationId());
                    
                    // Fetch operation details
                    operationRepository.findById(step.getOperationId()).ifPresent(op -> {
                        opData.put("name", op.getName());
                        opData.put("sequence", op.getSequence());
                        opData.put("stageGroup", step.getStageGroup());
                    });
                    
                    return opData;
                })
                .filter(op -> op.containsKey("name"))
                .sorted((a, b) -> {
                    Integer seqA = (Integer) a.getOrDefault("sequence", 0);
                    Integer seqB = (Integer) b.getOrDefault("sequence", 0);
                    return seqA.compareTo(seqB);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get bin current operation by tray QR code
     * Returns bin info including current_operation_id and operation name
     */
    public Map<String, Object> getBinCurrentOperation(String trayQr) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<Bin> binOpt = binRepository.findByQrCode(trayQr);
        
        if (!binOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "Bin not found for tray QR: " + trayQr);
            return response;
        }
        
        Bin bin = binOpt.get();
        
        response.put("success", true);
        response.put("binId", bin.getBinId());
        response.put("qrCode", bin.getQrCode());
        response.put("currentOperationId", bin.getCurrentOperationId());
        response.put("lastOperationId", bin.getLastOperationId());
        response.put("currentRoutingId", bin.getCurrentRoutingId());
        response.put("status", bin.getStatus());
        response.put("currentStatus", bin.getCurrentStatus());
        
        // Fetch operation name if current operation exists
        if (bin.getCurrentOperationId() != null) {
            operationRepository.findById(bin.getCurrentOperationId()).ifPresent(op -> {
                response.put("currentOperationName", op.getName());
                response.put("currentOperationSequence", op.getSequence());
            });
        }
        
        return response;
    }

    /**
     * Submit QR assignment using enhanced workflow
     * Includes transaction management, status validation, and assignment history
     */
    @Transactional
    public Map<String, Object> submitQrAssignment(QrAssignmentRequest request) {
        return enhancedQrAssignmentService.processEnhancedQrAssignment(request);
    }
    
    /**
     * Find style variant ID based on the provided data
     * Returns null if no matching variant found
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
     * Submit tracking data using enhanced two-phase workflow
     * Automatically detects assignment vs completion
     */
    @Transactional
    public Map<String, Object> submitTracking(TrackingRequest request) {
        return enhancedTrackingService.processTracking(request);
    }
    
    /**
     * Submit merging data using enhanced workflow
     * Includes compatibility validation and multi-table updates
     */
    @Transactional
    public Map<String, Object> submitMerging(MergingRequest request) {
        return enhancedMergingService.processEnhancedMerging(request);
    }
}
