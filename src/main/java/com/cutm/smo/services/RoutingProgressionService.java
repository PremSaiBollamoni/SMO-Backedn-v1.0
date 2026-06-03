package com.cutm.smo.services;

import com.cutm.smo.models.Bin;
import com.cutm.smo.models.RoutingStep;
import com.cutm.smo.models.RoutingEdge;
import com.cutm.smo.models.Order;
import com.cutm.smo.repositories.BinRepository;
import com.cutm.smo.repositories.RoutingStepRepository;
import com.cutm.smo.repositories.RoutingEdgeRepository;
import com.cutm.smo.repository.OrderRepository;
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

    @Autowired
    private RoutingEdgeRepository routingEdgeRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Get the next operation in routing sequence after the given operation
     * Uses routing_edge table for parallel paths with merges
     * @param routingId The routing ID
     * @param currentOperationId The current operation ID
     * @return Next operation ID, or null if this is the last operation
     */
    public Long getNextOperationInSequence(Long routingId, Long currentOperationId) {
        // Try to find edge from current operation
        List<RoutingEdge> edges = routingEdgeRepository.findByRoutingIdAndFromOperationId(routingId, currentOperationId);
        
        if (!edges.isEmpty()) {
            // Use first edge found (in case of multiple outgoing edges, take first)
            return edges.get(0).getToOperationId();
        }
        
        // Fallback to linear sequence if no edges found
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

            // Save bin first so the new COMPLETED status is included in the order roll-up
            binRepository.save(bin);

            // Update linked order's status if total completed qty across all bins
            // for that order has reached the order's target qty.
            updateOrderStatusIfComplete(bin.getOrderId(), result);
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
     * NOTE: Sequence validation REMOVED (NEW: support operation-wise tracking regardless of flow)
     * Allows operations to be completed in any order when workers are absent or flow is disturbed
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

        // SEQUENCE CHECK REMOVED - Allow any operation to be completed any time
        // This supports operation-wise tracking when workers are absent or flow is disturbed

        result.put("valid", true);
        result.put("message", "Operation can be completed");
        return result;
    }

    /**
     * Update order status to COMPLETED if total completed qty across all bins
     * linked to this order has reached or exceeded the order's target qty.
     * No-op if orderId is null or order not found.
     */
    private void updateOrderStatusIfComplete(Long orderId, Map<String, Object> result) {
        if (orderId == null) {
            return;
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (!orderOpt.isPresent()) {
            return;
        }

        Order order = orderOpt.get();

        // If already in a terminal state, skip
        if ("COMPLETED".equalsIgnoreCase(order.getStatus()) ||
            "CANCELLED".equalsIgnoreCase(order.getStatus())) {
            return;
        }

        // Sum completed qty across all bins linked to this order
        List<Bin> orderBins = binRepository.findByOrderId(orderId);
        int completedQty = 0;
        for (Bin b : orderBins) {
            if ("COMPLETED".equalsIgnoreCase(b.getStatus()) && b.getQty() != null) {
                completedQty += b.getQty();
            }
        }

        Integer orderQty = order.getOrderQty();
        result.put("orderId", orderId);
        result.put("orderCompletedQty", completedQty);
        result.put("orderTargetQty", orderQty);

        if (orderQty != null && completedQty >= orderQty) {
            order.setStatus("COMPLETED");
            orderRepository.save(order);
            result.put("orderStatusChanged", true);
            result.put("orderStatus", "COMPLETED");
        } else {
            // Move to IN_PROGRESS once any qty has been completed (and not yet there)
            if (completedQty > 0 && !"IN_PROGRESS".equalsIgnoreCase(order.getStatus())) {
                order.setStatus("IN_PROGRESS");
                orderRepository.save(order);
                result.put("orderStatusChanged", true);
                result.put("orderStatus", "IN_PROGRESS");
            } else {
                result.put("orderStatusChanged", false);
                result.put("orderStatus", order.getStatus());
            }
        }
    }
}
