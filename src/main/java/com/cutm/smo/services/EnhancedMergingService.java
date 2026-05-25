package com.cutm.smo.services;

import com.cutm.smo.dto.MergingRequest;
import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class EnhancedMergingService {

    @Autowired
    private BinRepository binRepository;

    @Autowired
    private TempBinMergeRepository tempBinMergeRepository;

    @Autowired
    private BinMergeHistoryRepository binMergeHistoryRepository;

    @Autowired
    private QrEventService qrEventService;

    @Autowired
    private StyleVariantRepository styleVariantRepository;

    /**
     * Enhanced merging with compatibility validation and multi-table updates
     */
    @Transactional
    public Map<String, Object> processEnhancedMerging(MergingRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Step 1: Basic validation
            Map<String, Object> validationResult = validateBasicRequirements(request);
            if (!(Boolean) validationResult.get("success")) {
                return validationResult;
            }

            // Step 2: Find bins by QR codes
            Optional<Bin> bin1Opt = binRepository.findByQrCode(request.getTub1Qr());
            Optional<Bin> bin2Opt = binRepository.findByQrCode(request.getTub2Qr());

            if (!bin1Opt.isPresent()) {
                response.put("success", false);
                response.put("message", "Tub 1 not found in system");
                response.put("errorType", "BIN_NOT_FOUND");
                return response;
            }

            if (!bin2Opt.isPresent()) {
                response.put("success", false);
                response.put("message", "Tub 2 not found in system");
                response.put("errorType", "BIN_NOT_FOUND");
                return response;
            }

            Bin targetBin = bin1Opt.get(); // Tub 1 (target)
            Bin sourceBin = bin2Opt.get(); // Tub 2 (source)

            // Step 3: Compatibility validation
            Map<String, Object> compatibilityResult = validateCompatibility(targetBin, sourceBin);
            if (!(Boolean) compatibilityResult.get("success")) {
                return compatibilityResult;
            }

            // Step 4: Execute multi-table merge transaction
            return executeMergeTransaction(request, targetBin, sourceBin);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing merge: " + e.getMessage());
            response.put("errorType", "SYSTEM_ERROR");
            return response;
        }
    }

    /**
     * Validate basic requirements
     */
    private Map<String, Object> validateBasicRequirements(MergingRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (request.getTub1Qr() == null || request.getTub1Qr().trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Tub 1 QR is required");
            response.put("errorType", "VALIDATION_ERROR");
            return response;
        }

        if (request.getTub2Qr() == null || request.getTub2Qr().trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Tub 2 QR is required");
            response.put("errorType", "VALIDATION_ERROR");
            return response;
        }

        if (request.getTub1Qr().equals(request.getTub2Qr())) {
            response.put("success", false);
            response.put("message", "Cannot merge the same tub");
            response.put("errorType", "VALIDATION_ERROR");
            return response;
        }

        response.put("success", true);
        return response;
    }

    /**
     * Validate compatibility between bins
     */
    private Map<String, Object> validateCompatibility(Bin targetBin, Bin sourceBin) {
        Map<String, Object> response = new HashMap<>();

        // Check if both bins are ACTIVE
        if (!"ACTIVE".equalsIgnoreCase(targetBin.getStatus())) {
            response.put("success", false);
            response.put("message", "Tub 1 is not active (Status: " + targetBin.getStatus() + ")");
            response.put("errorType", "STATUS_ERROR");
            return response;
        }

        if (!"ACTIVE".equalsIgnoreCase(sourceBin.getStatus())) {
            response.put("success", false);
            response.put("message", "Tub 2 is not active (Status: " + sourceBin.getStatus() + ")");
            response.put("errorType", "STATUS_ERROR");
            return response;
        }

        // Check style variant compatibility
        // Use currentStyleVariantId (new field) if available, fallback to styleVariantId (old field)
        Long targetVariantId = targetBin.getCurrentStyleVariantId() != null 
            ? targetBin.getCurrentStyleVariantId() 
            : targetBin.getStyleVariantId();
            
        Long sourceVariantId = sourceBin.getCurrentStyleVariantId() != null 
            ? sourceBin.getCurrentStyleVariantId() 
            : sourceBin.getStyleVariantId();
        
        if (targetVariantId == null || sourceVariantId == null) {
            response.put("success", false);
            response.put("message", "One or both tubs missing style variant information");
            response.put("errorType", "COMPATIBILITY_ERROR");
            return response;
        }

        if (!targetVariantId.equals(sourceVariantId)) {
            response.put("success", false);
            response.put("message", "Incompatible bins: Different style/size/color variants");
            response.put("errorType", "COMPATIBILITY_ERROR");
            response.put("targetVariantId", targetVariantId);
            response.put("sourceVariantId", sourceVariantId);
            return response;
        }

        // ========== DEFENSIVE GUARD: CHECK FOR NULL CRITICAL FIELDS ==========
        // Prevent merge if style_variant has incomplete data
        // This is a temporary guard until all data is properly populated
        // Use currentStyleVariantId (new field) if available, fallback to styleVariantId (old field)
        Long targetVariantIdForValidation = targetVariantId; // Already resolved above
        Long sourceVariantIdForValidation = sourceVariantId; // Already resolved above
        
        Optional<StyleVariant> targetVariantOpt = styleVariantRepository.findById(targetVariantIdForValidation);
        Optional<StyleVariant> sourceVariantOpt = styleVariantRepository.findById(sourceVariantIdForValidation);
        
        if (!targetVariantOpt.isPresent() || !sourceVariantOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "Style variant data not found. Cannot validate compatibility.");
            response.put("errorType", "DATA_ERROR");
            return response;
        }
        
        StyleVariant targetVariant = targetVariantOpt.get();
        StyleVariant sourceVariant = sourceVariantOpt.get();
        
        // Check for NULL critical fields in target variant
        if (targetVariant.getButtonId() == null || targetVariant.getThreadId() == null ||
            targetVariant.getMainLabel() == null || targetVariant.getBrandingLabel() == null) {
            response.put("success", false);
            response.put("message", "Tub 1 has incomplete style variant data (missing button/thread/labels). Please contact administrator.");
            response.put("errorType", "DATA_INTEGRITY_ERROR");
            return response;
        }
        
        // Check for NULL critical fields in source variant
        if (sourceVariant.getButtonId() == null || sourceVariant.getThreadId() == null ||
            sourceVariant.getMainLabel() == null || sourceVariant.getBrandingLabel() == null) {
            response.put("success", false);
            response.put("message", "Tub 2 has incomplete style variant data (missing button/thread/labels). Please contact administrator.");
            response.put("errorType", "DATA_INTEGRITY_ERROR");
            return response;
        }

        // Check quantities are valid
        Integer targetQty = targetBin.getQty() != null ? targetBin.getQty() : 0;
        Integer sourceQty = sourceBin.getQty() != null ? sourceBin.getQty() : 0;

        if (sourceQty <= 0) {
            response.put("success", false);
            response.put("message", "Source tub has no quantity to transfer");
            response.put("errorType", "QUANTITY_ERROR");
            return response;
        }

        response.put("success", true);
        response.put("targetQty", targetQty);
        response.put("sourceQty", sourceQty);
        return response;
    }

    /**
     * Execute the multi-table merge transaction
     */
    private Map<String, Object> executeMergeTransaction(MergingRequest request, Bin targetBin, Bin sourceBin) {
        Map<String, Object> response = new HashMap<>();

        Integer targetQty = targetBin.getQty() != null ? targetBin.getQty() : 0;
        Integer sourceQty = sourceBin.getQty() != null ? sourceBin.getQty() : 0;
        Integer totalQty = targetQty + sourceQty;
        Long supervisorId = request.getSupervisorId() != null ? request.getSupervisorId() : 1004L; // Default supervisor

        // Step 1: Insert into temp_bin_merges
        TempBinMerge tempMerge = new TempBinMerge();
        tempMerge.setSourceBinQr(request.getTub2Qr());
        tempMerge.setTargetBinQr(request.getTub1Qr());
        tempMerge.setSourceBinId(sourceBin.getBinId());
        tempMerge.setTargetBinId(targetBin.getBinId());
        tempMerge.setQtyTransferred(sourceQty);
        tempMerge.setMergedBy(supervisorId);
        tempMerge.setNotes(buildMergeNotes(request, targetBin, sourceBin));
        
        TempBinMerge savedTempMerge = tempBinMergeRepository.save(tempMerge);

        // Step 2: Update target bin quantity
        // Keep target bin ACTIVE so it can continue tracking or be merged with more bins
        targetBin.setQty(totalQty);
        binRepository.save(targetBin);

        // Step 3: Reset source bin to FREE state for reuse
        sourceBin.setStatus("FREE");
        sourceBin.setCurrentStatus("free");
        sourceBin.setQty(0);
        sourceBin.setCurrentRoutingId(null);
        sourceBin.setCurrentStyleVariantId(null);
        sourceBin.setCurrentOperationId(null);
        sourceBin.setLastOperationId(null);
        sourceBin.setAssignmentStartTime(null);
        sourceBin.setAssignmentEndTime(null);
        sourceBin.setLastAssignedBy(null);
        sourceBin.setOrderId(null);
        binRepository.save(sourceBin);

        // Step 4: Insert into bin_merge_history
        BinMergeHistory mergeHistory = new BinMergeHistory();
        Long nextMergeId = binMergeHistoryRepository.getNextMergeId();
        mergeHistory.setMergeId(nextMergeId);
        mergeHistory.setSourceBinId(sourceBin.getBinId());
        mergeHistory.setTargetBinId(targetBin.getBinId());
        mergeHistory.setQtyTransferred(sourceQty);
        mergeHistory.setMergedByEmpId(supervisorId);
        mergeHistory.setMergedAt(LocalDateTime.now());
        mergeHistory.setNotes(tempMerge.getNotes());
        
        BinMergeHistory savedHistory = binMergeHistoryRepository.save(mergeHistory);

        // Step 5: Log QR events for audit trail (both source and target)
        qrEventService.logQrEvent(
            request.getTub2Qr(),
            "MERGE",
            savedHistory.getMergeId(),
            "MERGE_SOURCE",
            null,
            null,
            supervisorId,
            null
        );
        
        qrEventService.logQrEvent(
            request.getTub1Qr(),
            "MERGE",
            savedHistory.getMergeId(),
            "MERGE_TARGET",
            null,
            null,
            supervisorId,
            null
        );

        // Step 6: Build success response
        response.put("success", true);
        response.put("message", "Tub 2 merged into Tub 1 successfully. Source tub freed for reuse.");
        response.put("mergedBinId", targetBin.getBinId());
        response.put("freedBinId", sourceBin.getBinId());
        response.put("totalQuantity", totalQty);
        response.put("qtyTransferred", sourceQty);
        response.put("tempMergeId", savedTempMerge.getMergeTempId());
        response.put("historyMergeId", savedHistory.getMergeId());
        response.put("mergedAt", savedTempMerge.getMergedAt());
        response.put("mergedBy", supervisorId);
        response.put("sourceBinStatus", "FREE");
        response.put("sourceBinReusable", true);

        return response;
    }

    /**
     * Build merge notes for audit trail
     */
    private String buildMergeNotes(MergingRequest request, Bin targetBin, Bin sourceBin) {
        StringBuilder notes = new StringBuilder();
        notes.append("Enhanced Merge Operation:\n");
        notes.append("Target: ").append(request.getTub1Qr()).append(" (").append(request.getTub1Description()).append(")\n");
        notes.append("Source: ").append(request.getTub2Qr()).append(" (").append(request.getTub2Description()).append(")\n");
        notes.append("Style Variant ID: ").append(targetBin.getStyleVariantId()).append("\n");
        notes.append("Target Qty Before: ").append(targetBin.getQty()).append("\n");
        notes.append("Source Qty Transferred: ").append(sourceBin.getQty()).append("\n");
        notes.append("Target Qty After: ").append((targetBin.getQty() != null ? targetBin.getQty() : 0) + (sourceBin.getQty() != null ? sourceBin.getQty() : 0));
        
        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            notes.append("\nAdditional Notes: ").append(request.getNotes());
        }
        
        return notes.toString();
    }
}