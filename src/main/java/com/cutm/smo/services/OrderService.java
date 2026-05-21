package com.cutm.smo.services;

import com.cutm.smo.models.Order;
import com.cutm.smo.repository.OrderRepository;
import com.cutm.smo.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final RoutingRepository routingRepository;
    private final ProductRepository productRepository;
    private final RoutingStepRepository routingStepRepository;
    private final WipTrackingRepository wipTrackingRepository;
    private final BinRepository binRepository;
    
    public OrderService(
            OrderRepository orderRepository,
            RoutingRepository routingRepository,
            ProductRepository productRepository,
            RoutingStepRepository routingStepRepository,
            WipTrackingRepository wipTrackingRepository,
            BinRepository binRepository) {
        this.orderRepository = orderRepository;
        this.routingRepository = routingRepository;
        this.productRepository = productRepository;
        this.routingStepRepository = routingStepRepository;
        this.wipTrackingRepository = wipTrackingRepository;
        this.binRepository = binRepository;
    }
    
    @Transactional
    public Order createOrder(Order order) {
        log.info("[OrderService] Creating order: {}", order.getOrderNumber());
        
        // Validate order number uniqueness
        if (orderRepository.existsByOrderNumber(order.getOrderNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Order number already exists: " + order.getOrderNumber());
        }
        
        // Validate product exists
        if (!productRepository.existsById(order.getProductId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Product not found: " + order.getProductId());
        }
        
        // Validate routing exists and is APPROVED
        var routing = routingRepository.findById(order.getRoutingId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Routing not found: " + order.getRoutingId()));
        
        if (!"APPROVED".equalsIgnoreCase(routing.getApprovalStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Only APPROVED routings can be linked to orders. Routing " + 
                order.getRoutingId() + " has status: " + routing.getApprovalStatus());
        }
        
        // Set default status if not provided
        if (order.getStatus() == null || order.getStatus().isEmpty()) {
            order.setStatus("DRAFT");
        }
        
        Order savedOrder = orderRepository.save(order);
        log.info("[OrderService] Order created successfully: {} (ID: {})", 
            savedOrder.getOrderNumber(), savedOrder.getOrderId());
        
        return savedOrder;
    }
    
    @Transactional
    public Order activateOrder(Long orderId) {
        log.info("[OrderService] Activating order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Order not found: " + orderId));
        
        if (!"DRAFT".equalsIgnoreCase(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Only DRAFT orders can be activated. Current status: " + order.getStatus());
        }
        
        order.setStatus("ACTIVE");
        order.setProductionStartDate(LocalDate.now());
        
        Order savedOrder = orderRepository.save(order);
        log.info("[OrderService] Order activated: {}", savedOrder.getOrderNumber());
        
        return savedOrder;
    }
    
    public List<Order> getActiveOrders() {
        return orderRepository.findActiveOrders();
    }
    
    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }
    
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Order not found: " + orderId));
    }
    
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Order not found: " + orderNumber));
    }
    
    /**
     * Get order status with real-time progress calculation
     */
    public Map<String, Object> getOrderStatus(Long orderId) {
        log.info("[OrderService] Getting status for order: {}", orderId);
        
        Order order = getOrderById(orderId);
        
        // Get product name
        String productName = productRepository.findById(order.getProductId())
            .map(p -> p.getName())
            .orElse("Unknown Product");
        
        // Calculate progress from WIP tracking
        Map<String, Object> progress = calculateOrderProgress(orderId, order.getRoutingId(), order.getOrderQty());
        
        Map<String, Object> status = new HashMap<>();
        status.put("order_id", order.getOrderNumber());
        status.put("order_qty", order.getOrderQty());
        status.put("completed", progress.get("completed_qty"));
        status.put("pending", progress.get("pending_qty"));
        status.put("progress_percent", progress.get("progress_percent"));
        status.put("expected_completion_date", order.getExpectedCompletionDate());
        status.put("avg_time_per_unit", progress.get("avg_time_per_unit"));
        status.put("routing_id", order.getRoutingId());
        status.put("product_id", order.getProductId());
        status.put("product_name", productName);
        status.put("customer_name", order.getCustomerName());
        status.put("status", order.getStatus());
        status.put("production_start_date", order.getProductionStartDate());
        
        return status;
    }
    
    /**
     * Calculate order progress from wiptracking table
     * Uses terminal routing step (last operation in routing) to determine completion
     */
    private Map<String, Object> calculateOrderProgress(Long orderId, Long routingId, Integer orderQty) {
        Map<String, Object> result = new HashMap<>();
        
        // Find terminal operation (last step in routing by sequence)
        var routingSteps = routingStepRepository.findByRoutingIdOrderByRoutingStepIdAsc(routingId);
        
        if (routingSteps.isEmpty()) {
            result.put("completed_qty", 0);
            result.put("pending_qty", orderQty);
            result.put("progress_percent", 0.0);
            result.put("avg_time_per_unit", "N/A");
            return result;
        }
        
        // Terminal operation is the last step
        Long terminalOperationId = routingSteps.get(routingSteps.size() - 1).getOperationId();
        
        // Query wiptracking for completed quantity at terminal operation
        // Join: wiptracking -> bin -> order
        // Count completed units where bin.order_id = orderId AND operation_id = terminalOperationId
        int completedQty = calculateCompletedQuantity(orderId, terminalOperationId);
        
        // Calculate average time per unit from wiptracking
        String avgTimePerUnit = calculateAverageTimePerUnit(orderId, terminalOperationId);
        
        int pendingQty = orderQty - completedQty;
        double progressPercent = orderQty > 0 ? (completedQty * 100.0 / orderQty) : 0.0;
        
        result.put("completed_qty", completedQty);
        result.put("pending_qty", pendingQty);
        result.put("progress_percent", Math.round(progressPercent * 10.0) / 10.0);
        result.put("avg_time_per_unit", avgTimePerUnit);
        result.put("terminal_operation_id", terminalOperationId);
        
        log.debug("[OrderService] Progress for order {}: {}% ({}/{})", 
            orderId, progressPercent, completedQty, orderQty);
        
        return result;
    }
    
    /**
     * Calculate completed quantity for an order at terminal operation
     * Sums qty from wiptracking where bin.order_id matches and operation is terminal
     */
    private int calculateCompletedQuantity(Long orderId, Long terminalOperationId) {
        try {
            // Get bins for this order
            List<com.cutm.smo.models.Bin> bins = binRepository.findByOrderId(orderId);
            
            if (bins.isEmpty()) {
                log.debug("[OrderService] No bins found for order {}", orderId);
                return 0;
            }
            
            List<Long> binIds = bins.stream()
                .map(bin -> bin.getBinId())
                .toList();
            
            log.debug("[OrderService] Found {} bins for order {}: {}", bins.size(), orderId, binIds);
            
            // Sum completed quantities from wiptracking for these bins at terminal operation
            int totalCompleted = wipTrackingRepository.findAll().stream()
                .filter(wip -> binIds.contains(wip.getBinId()))
                .filter(wip -> terminalOperationId.equals(wip.getOperationId()))
                .filter(wip -> "completed".equalsIgnoreCase(wip.getStatus()))
                .mapToInt(wip -> wip.getQty() != null ? wip.getQty() : 0)
                .sum();
            
            log.info("[OrderService] Completed qty for order {} at terminal op {}: {} (from {} bins)", 
                orderId, terminalOperationId, totalCompleted, bins.size());
            
            return totalCompleted;
        } catch (Exception e) {
            log.error("[OrderService] Error calculating completed quantity for order {}: {}", 
                orderId, e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Calculate average time per unit from wiptracking
     * Uses completed records at terminal operation
     */
    private String calculateAverageTimePerUnit(Long orderId, Long terminalOperationId) {
        try {
            List<com.cutm.smo.models.Bin> bins = binRepository.findByOrderId(orderId);
            
            if (bins.isEmpty()) {
                return "N/A";
            }
            
            List<Long> binIds = bins.stream()
                .map(bin -> bin.getBinId())
                .toList();
            
            // Get completed wiptracking records for these bins at terminal operation
            var completedRecords = wipTrackingRepository.findAll().stream()
                .filter(wip -> binIds.contains(wip.getBinId()))
                .filter(wip -> terminalOperationId.equals(wip.getOperationId()))
                .filter(wip -> "completed".equalsIgnoreCase(wip.getStatus()))
                .filter(wip -> wip.getStartTime() != null && wip.getEndTime() != null)
                .toList();
            
            if (completedRecords.isEmpty()) {
                return "N/A";
            }
            
            // Calculate total time and total quantity
            long totalMinutes = 0;
            int totalQty = 0;
            
            for (var record : completedRecords) {
                Duration duration = Duration.between(record.getStartTime(), record.getEndTime());
                totalMinutes += duration.toMinutes();
                totalQty += record.getQty() != null ? record.getQty() : 0;
            }
            
            if (totalQty == 0) {
                return "N/A";
            }
            
            double avgMinutesPerUnit = (double) totalMinutes / totalQty;
            
            // Format as human-readable
            if (avgMinutesPerUnit < 1) {
                return String.format("%.1f sec", avgMinutesPerUnit * 60);
            } else if (avgMinutesPerUnit < 60) {
                return String.format("%.1f min", avgMinutesPerUnit);
            } else {
                return String.format("%.1f hrs", avgMinutesPerUnit / 60);
            }
        } catch (Exception e) {
            log.error("[OrderService] Error calculating avg time for order {}: {}", 
                orderId, e.getMessage(), e);
            return "N/A";
        }
    }
    
    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatus) {
        Order order = getOrderById(orderId);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }
}
