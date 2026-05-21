package com.cutm.smo.controller;

import com.cutm.smo.models.Order;
import com.cutm.smo.services.AccessControlService;
import com.cutm.smo.services.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    private final OrderService orderService;
    private final AccessControlService accessControlService;
    
    public OrderController(OrderService orderService, AccessControlService accessControlService) {
        this.orderService = orderService;
        this.accessControlService = accessControlService;
    }
    
    /**
     * Create new order (GM only)
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestBody Order order,
            @RequestParam String actorEmpId) {
        
        log.info("[OrderController] Create order request from employee: {}", actorEmpId);
        
        // Check GM permission
        accessControlService.require(actorEmpId, "PP_APPROVE");
        
        // Set created_by from actorEmpId
        order.setCreatedBy(Long.parseLong(actorEmpId));
        
        Order createdOrder = orderService.createOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }
    
    /**
     * Activate order (GM only)
     */
    @PostMapping("/{orderId}/activate")
    public ResponseEntity<Map<String, Object>> activateOrder(
            @PathVariable Long orderId,
            @RequestParam String actorEmpId) {
        
        log.info("[OrderController] Activate order {} request from employee: {}", orderId, actorEmpId);
        
        // Check GM permission
        accessControlService.require(actorEmpId, "PP_APPROVE");
        
        Order activatedOrder = orderService.activateOrder(orderId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("order_id", activatedOrder.getOrderId());
        response.put("order_number", activatedOrder.getOrderNumber());
        response.put("status", activatedOrder.getStatus());
        response.put("production_start_date", activatedOrder.getProductionStartDate());
        response.put("message", "Order activated successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get active orders (GM/Supervisor)
     */
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveOrders(@RequestParam String actorEmpId) {
        
        log.info("[OrderController] Get active orders request from employee: {}", actorEmpId);
        
        // Check permission (GM or Supervisor)
        try {
            accessControlService.require(actorEmpId, "PP_APPROVE");
        } catch (Exception e1) {
            try {
                accessControlService.require(actorEmpId, "SUPERVISOR_MONITOR_WIP");
            } catch (Exception e2) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Access denied: Requires PP_APPROVE or SUPERVISOR_MONITOR_WIP activity");
            }
        }
        
        List<Order> orders = orderService.getActiveOrders();
        
        // Transform to response format
        List<Map<String, Object>> response = orders.stream()
            .map(order -> {
                Map<String, Object> orderMap = new HashMap<>();
                orderMap.put("order_id", order.getOrderId());
                orderMap.put("order_number", order.getOrderNumber());
                orderMap.put("routing_id", order.getRoutingId());
                orderMap.put("product_id", order.getProductId());
                orderMap.put("product_name", "Product #" + order.getProductId());
                orderMap.put("order_qty", order.getOrderQty());
                orderMap.put("status", order.getStatus());
                orderMap.put("customer_name", order.getCustomerName());
                return orderMap;
            })
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get order status with progress (GM/Supervisor)
     * Updated to accept order_number instead of orderId for frontend compatibility
     */
    @GetMapping("/status/{orderNumber}")
    public ResponseEntity<Map<String, Object>> getOrderStatusByNumber(
            @PathVariable String orderNumber,
            @RequestParam String actorEmpId) {
        
        log.info("[OrderController] Get order status {} request from employee: {}", orderNumber, actorEmpId);
        
        // Check permission (GM or Supervisor)
        try {
            accessControlService.require(actorEmpId, "PP_APPROVE");
        } catch (Exception e1) {
            try {
                accessControlService.require(actorEmpId, "SUPERVISOR_MONITOR_WIP");
            } catch (Exception e2) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Access denied: Requires PP_APPROVE or SUPERVISOR_MONITOR_WIP activity");
            }
        }
        
        // Find order by order_number
        Order order = orderService.getOrderByNumber(orderNumber);
        Map<String, Object> status = orderService.getOrderStatus(order.getOrderId());
        return ResponseEntity.ok(status);
    }
    
    /**
     * Get order status with progress (GM/Supervisor)
     */
    @GetMapping("/{orderId}/status")
    public ResponseEntity<Map<String, Object>> getOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String actorEmpId) {
        
        log.info("[OrderController] Get order status {} request from employee: {}", orderId, actorEmpId);
        
        // Check permission (GM or Supervisor)
        try {
            accessControlService.require(actorEmpId, "PP_APPROVE");
        } catch (Exception e1) {
            try {
                accessControlService.require(actorEmpId, "SUPERVISOR_MONITOR_WIP");
            } catch (Exception e2) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Access denied: Requires PP_APPROVE or SUPERVISOR_MONITOR_WIP activity");
            }
        }
        
        Map<String, Object> status = orderService.getOrderStatus(orderId);
        return ResponseEntity.ok(status);
    }
    
    /**
     * Get all orders by status (GM only)
     */
    @GetMapping
    public ResponseEntity<List<Order>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam String actorEmpId) {
        
        log.info("[OrderController] Get orders request from employee: {}", actorEmpId);
        
        // Check GM permission
        accessControlService.require(actorEmpId, "PP_APPROVE");
        
        List<Order> orders = status != null ? 
            orderService.getOrdersByStatus(status) : 
            orderService.getActiveOrders();
        
        return ResponseEntity.ok(orders);
    }
}
