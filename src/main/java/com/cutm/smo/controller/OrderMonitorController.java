package com.cutm.smo.controller;

import com.cutm.smo.services.AccessControlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class OrderMonitorController {
    private final AccessControlService accessControlService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public OrderMonitorController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @GetMapping("/approved-orders")
    public List<Map<String, Object>> getApprovedOrders(
            @RequestParam String actorEmpId,
            @RequestParam(required = false) Long routingId) {
        log.info("=== GET APPROVED ORDERS START ===");
        log.debug("Actor Employee ID: {}", actorEmpId);
        log.debug("Routing ID filter: {}", routingId);

        // Check access (GM or Supervisor)
        try {
            accessControlService.require(actorEmpId, "PP_APPROVE");
        } catch (Exception e1) {
            try {
                accessControlService.require(actorEmpId, "SUPERVISOR_MONITOR_WIP");
            } catch (Exception e2) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Access denied: Requires PP_APPROVE or SUPERVISOR_MONITOR_WIP activity"
                );
            }
        }

        List<Map<String, Object>> orders;

        if (routingId != null) {
            // Filter by specific routing/process plan — no active bins requirement
            // Also check the routing chain: order may be linked to the draft routing
            // while the approved routing has a different ID (previous_routing_id chain)
            String sql = "SELECT o.order_id, o.order_number, o.customer_name, " +
                        "o.routing_id, o.product_id, o.order_qty, o.status, " +
                        "o.production_start_date, o.expected_completion_date " +
                        "FROM orders o " +
                        "WHERE o.routing_id = ? " +
                        "OR o.routing_id IN (" +
                        "  SELECT r2.routing_id FROM routing r2 WHERE r2.previous_routing_id = ?" +
                        ") " +
                        "ORDER BY o.order_number";
            orders = jdbcTemplate.queryForList(sql, routingId, routingId);
            log.info("Found {} orders for routingId={} (including routing chain)", orders.size(), routingId);
        } else {
            // No filter — return all orders that have active bins (original behaviour)
            String sql = "SELECT DISTINCT o.order_id, o.order_number, o.customer_name, " +
                        "o.routing_id, o.product_id, o.order_qty, o.status " +
                        "FROM orders o " +
                        "JOIN bin b ON o.order_id = b.order_id " +
                        "WHERE b.status IN ('ACTIVE', 'assigned') " +
                        "ORDER BY o.order_number";
            orders = jdbcTemplate.queryForList(sql);
        }

        log.info("Retrieved {} approved orders", orders.size());
        log.info("=== GET APPROVED ORDERS END - SUCCESS ===");
        return orders;
    }

    @GetMapping("/order-stats")
    public Map<String, Object> getOrderStats(
            @RequestParam String actorEmpId,
            @RequestParam(required = false) Long orderId) {
        log.info("=== GET ORDER STATS START ===");
        log.debug("Actor Employee ID: {}", actorEmpId);
        log.debug("Order ID: {}", orderId);

        // Check access (GM or Supervisor)
        try {
            accessControlService.require(actorEmpId, "PP_APPROVE");
        } catch (Exception e1) {
            try {
                accessControlService.require(actorEmpId, "SUPERVISOR_MONITOR_WIP");
            } catch (Exception e2) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Access denied: Requires PP_APPROVE or SUPERVISOR_MONITOR_WIP activity"
                );
            }
        }

        Map<String, Object> stats = new HashMap<>();

        if (orderId != null) {
            // Stats for specific order
            String sql = "SELECT " +
                        "(SELECT COUNT(*) FROM bin WHERE status IN ('ACTIVE', 'assigned') AND order_id = ?) as active_bins, " +
                        "(SELECT COALESCE(SUM(qty), 0) FROM bin WHERE status IN ('ACTIVE', 'assigned') AND order_id = ?) as wip_quantity, " +
                        "(SELECT COUNT(*) FROM wiptracking w " +
                        " JOIN bin b ON w.bin_id = b.bin_id " +
                        " WHERE b.order_id = ? AND DATE(w.end_time) = CURDATE()) as today_operations, " +
                        "(SELECT COUNT(*) FROM bin_merge_history bmh " +
                        " JOIN bin b ON bmh.target_bin_id = b.bin_id " +
                        " WHERE b.order_id = ? AND DATE(bmh.merged_at) = CURDATE()) as today_merges, " +
                        "(SELECT COUNT(DISTINCT w.operator_id) FROM wiptracking w " +
                        " JOIN bin b ON w.bin_id = b.bin_id " +
                        " WHERE b.order_id = ? AND DATE(w.end_time) = CURDATE()) as active_operators";
            
            Map<String, Object> result = jdbcTemplate.queryForMap(sql, orderId, orderId, orderId, orderId, orderId);
            stats.putAll(result);
            stats.put("order_id", orderId);
        } else {
            // Total stats across all orders
            String sql = "SELECT " +
                        "(SELECT COUNT(*) FROM bin WHERE status IN ('ACTIVE', 'assigned')) as active_bins, " +
                        "(SELECT COALESCE(SUM(qty), 0) FROM bin WHERE status IN ('ACTIVE', 'assigned')) as wip_quantity, " +
                        "(SELECT COUNT(*) FROM wiptracking WHERE DATE(end_time) = CURDATE()) as today_operations, " +
                        "(SELECT COUNT(*) FROM bin_merge_history WHERE DATE(merged_at) = CURDATE()) as today_merges, " +
                        "(SELECT COUNT(DISTINCT operator_id) FROM wiptracking WHERE DATE(end_time) = CURDATE()) as active_operators";
            
            Map<String, Object> result = jdbcTemplate.queryForMap(sql);
            stats.putAll(result);
            stats.put("order_id", "ALL");
        }

        log.info("Retrieved order stats: {}", stats);
        log.info("=== GET ORDER STATS END - SUCCESS ===");
        return stats;
    }

    @GetMapping("/order-status/{routingId}")
    public Map<String, Object> getOrderStatus(
            @PathVariable Long routingId,
            @RequestParam String actorEmpId) {
        log.info("=== GET ORDER STATUS START ===");
        log.debug("Actor Employee ID: {}", actorEmpId);
        log.debug("Routing ID: {}", routingId);

        // Check access (GM or Supervisor)
        try {
            accessControlService.require(actorEmpId, "PP_APPROVE");
        } catch (Exception e1) {
            try {
                accessControlService.require(actorEmpId, "SUPERVISOR_MONITOR_WIP");
            } catch (Exception e2) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Access denied: Requires PP_APPROVE or SUPERVISOR_MONITOR_WIP activity"
                );
            }
        }

        // Mock data - replace with actual database query
        Map<String, Object> status = new HashMap<>();
        status.put("order_id", "ORD-2026-045");
        status.put("order_qty", 500);
        status.put("completed", 325);
        status.put("pending", 175);
        status.put("progress_percent", 65.0);
        status.put("expected_completion_date", LocalDate.now().plusDays(5).toString());
        status.put("avg_time_per_unit", "12.5 min");

        log.info("Retrieved order status for routing: {}", routingId);
        log.info("=== GET ORDER STATUS END - SUCCESS ===");
        return status;
    }
}
