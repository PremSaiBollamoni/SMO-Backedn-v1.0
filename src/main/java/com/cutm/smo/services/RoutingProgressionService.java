package com.cutm.smo.services;

import com.cutm.smo.models.Bin;
import com.cutm.smo.models.RoutingStep;
import com.cutm.smo.repositories.BinRepository;
import com.cutm.smo.repositories.RoutingStepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing routing progression and workflow state transitions
 */
@Service
public class RoutingProgressionService {

    @Autowired
    private RoutingStepRepository routingStepRepository;

    @Autowired
    private BinRepository binRepository;

    /**
     * Get the next operation in routing sequence after the given operation
     * @param routingId The routing ID
     * @param currentOperationId The current operation ID
     * @return Next operation ID, or null if this is the last operation
     */
    public Long getNextOperationInSequence(Long routingId, Long currentOperationId) {
        List<RoutingStep> steps = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(routingId);
        
        if (steps.isEmpty()) {
            return null;
        }

        // Find current operation index
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getOperationId().equals(currentOperationId)) {
                // Check if there's a next operation
                if (i < steps.size() - 1) {
                    return steps.get(i + 1).getOperationId();
                } else {
                    // This is the last operation
                    return null;
                }
            }
        }

        // Current operation not found in routing - return first operation as fallback
        return steps.get(0).getOperationId();
    }

    /**
     * Get the first operation in a routing sequence
     * @param routingId The routing ID
     * @return First operation ID, or null if routing has no steps
     */
    public Long getFirstOperationInSequence(Long routingId) {
        List<RoutingStep> steps = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(routingId);
        
        if (steps.isEmpty()) {
            return null;
        }

        return steps.get(0).getOperationId();
    }

    /**
     * Check if the given operation is the last operation in the routing
     * @param routingId The routing ID
     * @param operationId The operation ID to check
     * @return true if this is the last operation, false otherwise
     */
    public boolean isLastOperationInRouting(Long routingId, Long operationId) {
        List<RoutingStep> steps = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(routingId);
        
        if (steps.isEmpty()) {
            return true; // No steps means any operation is "last"
        }

        Long lastOperationId = steps.get(steps.size() - 1).getOperationId();
        return lastOperationId.equals(operationId);
    }

    /**
     * Advance bin to next operation in routing sequence
     * @param binId The bin ID
     * @param completedOperationId The operation that was just completed
     * @return Map with progression details
     */
    @Transactional
    public Map<String, Object> advanceToNextOperation(Long binId, Long completedOperationId) {
        Map<String, Object> result = new HashMap<>();

        Optional<Bin> binOpt = binRepository.findById(binId);
        if (!binOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "Bin not found");
            return result;
        }

        Bin bin = binOpt.get();

        // Validate bin has a routing assigned
        if (bin.getCurrentRoutingId() == null) {
            result.put("success", false);
            result.put("message", "Bin has no routing assigned");
            return result;
        }

        // Update last completed operation
        bin.setLastOperationId(completedOperationId);

        // Check if this is the last operation
        boolean isLastOp = isLastOperationInRouting(bin.getCurrentRoutingId(), completedOperationId);

        if (isLastOp) {
            // Workflow complete
            bin.setCurrentOperationId(null);
            bin.setStatus("COMPLETED");
            bin.setCurrentStatus("completed");
            bin.setCompletedAt(LocalDateTime.now());
            bin.setAssignmentEndTime(LocalDateTime.now());

            result.put("success", true);
            result.put("workflowComplete", true);
            result.put("message", "Workflow completed - all operations finished");
            result.put("completedAt", bin.getCompletedAt());
        } else {
            // Get next operation
            Long nextOperationId = getNextOperationInSequence(bin.getCurrentRoutingId(), completedOperationId);
            
            if (nextOperationId != null) {
                bin.setCurrentOperationId(nextOperationId);
                
                result.put("success", true);
                result.put("workflowComplete", false);
                result.put("message", "Advanced to next operation");
                result.put("nextOperationId", nextOperationId);
            } else {
                // Shouldn't happen if isLastOp check works correctly
                bin.setCurrentOperationId(null);
                result.put("success", true);
                result.put("workflowComplete", true);
                result.put("message", "No next operation found - workflow complete");
            }
        }

        binRepository.save(bin);

        result.put("binId", binId);
        result.put("completedOperationId", completedOperationId);
        result.put("currentOperationId", bin.getCurrentOperationId());
        result.put("lastOperationId", bin.getLastOperationId());
        result.put("status", bin.getStatus());

        return result;
    }

    /**
     * Initialize bin's current operation when first assigned
     * @param binId The bin ID
     * @param routingId The routing ID
     * @return First operation ID
     */
    @Transactional
    public Long initializeRoutingProgression(Long binId, Long routingId) {
        Optional<Bin> binOpt = binRepository.findById(binId);
        if (!binOpt.isPresent()) {
            return null;
        }

        Bin bin = binOpt.get();
        Long firstOperationId = getFirstOperationInSequence(routingId);
        
        if (firstOperationId != null) {
            bin.setCurrentOperationId(firstOperationId);
            binRepository.save(bin);
        }

        return firstOperationId;
    }

    /**
     * Get routing progress percentage for a bin
     * @param binId The bin ID
     * @return Progress percentage (0-100)
     */
    public int getRoutingProgress(Long binId) {
        Optional<Bin> binOpt = binRepository.findById(binId);
        if (!binOpt.isPresent()) {
            return 0;
        }

        Bin bin = binOpt.get();
        
        if (bin.getCurrentRoutingId() == null) {
            return 0;
        }

        List<RoutingStep> steps = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(bin.getCurrentRoutingId());
        
        if (steps.isEmpty()) {
            return 0;
        }

        // If workflow is completed
        if ("COMPLETED".equalsIgnoreCase(bin.getStatus()) || "completed".equalsIgnoreCase(bin.getCurrentStatus())) {
            return 100;
        }

        // Find how many operations have been completed
        if (bin.getLastOperationId() == null) {
            return 0; // No operations completed yet
        }

        int completedIndex = -1;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getOperationId().equals(bin.getLastOperationId())) {
                completedIndex = i;
                break;
            }
        }

        if (completedIndex == -1) {
            return 0;
        }

        // Calculate percentage: (completed operations / total operations) * 100
        int completedOps = completedIndex + 1;
        int totalOps = steps.size();
        
        return (completedOps * 100) / totalOps;
    }

    /**
     * Validate if bin can complete the given operation
     * @param binId The bin ID
     * @param operationId The operation to complete
     * @return Validation result
     */
    public Map<String, Object> validateOperationCompletion(Long binId, Long operationId) {
        Map<String, Object> result = new HashMap<>();

        Optional<Bin> binOpt = binRepository.findById(binId);
        if (!binOpt.isPresent()) {
            result.put("valid", false);
            result.put("message", "Bin not found");
            return result;
        }

        Bin bin = binOpt.get();

        // Check if bin has routing assigned
        if (bin.getCurrentRoutingId() == null) {
            result.put("valid", false);
            result.put("message", "Bin has no routing assigned");
            return result;
        }

        // Check if bin is already completed
        if ("COMPLETED".equalsIgnoreCase(bin.getStatus())) {
            result.put("valid", false);
            result.put("message", "Bin workflow is already completed");
            return result;
        }

        // Check if operation matches current operation
        if (bin.getCurrentOperationId() != null && !bin.getCurrentOperationId().equals(operationId)) {
            result.put("valid", false);
            result.put("message", "Operation out of sequence. Current operation: " + bin.getCurrentOperationId() + ", Attempted: " + operationId);
            result.put("currentOperationId", bin.getCurrentOperationId());
            result.put("attemptedOperationId", operationId);
            return result;
        }

        result.put("valid", true);
        result.put("message", "Operation can be completed");
        return result;
    }
}
