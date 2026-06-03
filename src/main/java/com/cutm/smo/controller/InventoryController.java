package com.cutm.smo.controller;

import com.cutm.smo.dto.DailyLedgerDto;
import com.cutm.smo.dto.OperationStockView;
import com.cutm.smo.dto.StockLimitRequest;
import com.cutm.smo.models.DailyStockLedger;
import com.cutm.smo.models.OperationStockLimit;
import com.cutm.smo.services.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    // Get dashboard data (actual vs target)
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @RequestParam(required = false) Long routingId) {
        List<OperationStockView> stockView;
        
        if (routingId != null) {
            // Filter by routing - get operations for this routing
            stockView = inventoryService.getOperationStockViewByRouting(routingId);
        } else {
            // Get all operations
            stockView = inventoryService.getOperationStockView();
        }
        
        // Calculate summary stats
        int totalOperations = stockView.size();
        long lowStockCount = stockView.stream()
                .filter(s -> "LOW".equals(s.getStockStatus()))
                .count();
        long highStockCount = stockView.stream()
                .filter(s -> "HIGH".equals(s.getStockStatus()))
                .count();
        long notSetCount = stockView.stream()
                .filter(s -> "NOT_SET".equals(s.getStockStatus()))
                .count();
        
        Map<String, Object> response = new HashMap<>();
        response.put("operations", stockView);
        response.put("summary", Map.of(
                "total", totalOperations,
                "lowStock", lowStockCount,
                "highStock", highStockCount,
                "notSet", notSetCount,
                "normal", totalOperations - lowStockCount - highStockCount - notSetCount
        ));
        
        return ResponseEntity.ok(response);
    }

    // Get all stock limits
    @GetMapping("/stock-limits")
    public ResponseEntity<List<OperationStockLimit>> getAllStockLimits() {
        return ResponseEntity.ok(inventoryService.getAllStockLimits());
    }

    // Get stock limit by operation ID
    @GetMapping("/stock-limits/{operationId}")
    public ResponseEntity<OperationStockLimit> getStockLimit(@PathVariable Long operationId) {
        OperationStockLimit limit = inventoryService.getStockLimitByOperationId(operationId);
        if (limit != null) {
            return ResponseEntity.ok(limit);
        }
        return ResponseEntity.notFound().build();
    }

    // Create or update stock limit
    @PostMapping("/stock-limits")
    public ResponseEntity<Map<String, Object>> saveStockLimit(@RequestBody StockLimitRequest request) {
        try {
            OperationStockLimit saved = inventoryService.saveStockLimit(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stock limit saved successfully");
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to save stock limit: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Update stock limit (same as create)
    @PutMapping("/stock-limits/{operationId}")
    public ResponseEntity<Map<String, Object>> updateStockLimit(
            @PathVariable Long operationId,
            @RequestBody StockLimitRequest request) {
        request.setOperationId(operationId);
        return saveStockLimit(request);
    }

    // Delete stock limit
    @DeleteMapping("/stock-limits/{operationId}")
    public ResponseEntity<Map<String, Object>> deleteStockLimit(@PathVariable Long operationId) {
        try {
            inventoryService.deleteStockLimit(operationId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stock limit deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete stock limit: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Get daily ledger for specific date
    @GetMapping("/daily-ledger")
    public ResponseEntity<List<DailyLedgerDto>> getDailyLedger(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(inventoryService.getDailyLedger(date));
    }

    // Get daily ledger for date range
    @GetMapping("/daily-ledger/range")
    public ResponseEntity<List<DailyLedgerDto>> getDailyLedgerRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(inventoryService.getDailyLedgerRange(startDate, endDate));
    }

    // Get operations list for dropdown
    @GetMapping("/operations")
    public ResponseEntity<List<Map<String, Object>>> getOperations() {
        return ResponseEntity.ok(inventoryService.getOperationsForDropdown());
    }

    // Get approved routings for inventory filtering (no access control - inventory-specific)
    @GetMapping("/routings")
    public ResponseEntity<List<Map<String, Object>>> getRoutingsForInventory() {
        return ResponseEntity.ok(inventoryService.getRoutingsForDropdown());
    }
}
