package com.cutm.smo.controller;

import com.cutm.smo.dto.AddRawMaterialRequest;
import com.cutm.smo.dto.RawMaterialStockDto;
import com.cutm.smo.dto.StockMovementRequest;
import com.cutm.smo.models.RawMaterialInventory;
import com.cutm.smo.models.StockMovement;
import com.cutm.smo.services.StockManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins = "*")
public class StockManagementController {

    @Autowired
    private StockManagementService stockService;

    // Get all raw materials
    @GetMapping("/raw-materials")
    public ResponseEntity<List<RawMaterialStockDto>> getAllRawMaterials() {
        return ResponseEntity.ok(stockService.getAllRawMaterials());
    }

    // Get raw material by ID
    @GetMapping("/raw-materials/{id}")
    public ResponseEntity<RawMaterialStockDto> getRawMaterialById(@PathVariable Long id) {
        RawMaterialStockDto dto = stockService.getRawMaterialById(id);
        if (dto != null) {
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }

    // Get materials by type
    @GetMapping("/raw-materials/type/{type}")
    public ResponseEntity<List<RawMaterialStockDto>> getRawMaterialsByType(@PathVariable String type) {
        return ResponseEntity.ok(stockService.getRawMaterialsByType(type));
    }

    // Get low stock materials
    @GetMapping("/raw-materials/low-stock")
    public ResponseEntity<List<RawMaterialStockDto>> getLowStockMaterials() {
        return ResponseEntity.ok(stockService.getLowStockMaterials());
    }

    // Add new raw material
    @PostMapping("/raw-materials")
    public ResponseEntity<Map<String, Object>> addRawMaterial(@RequestBody AddRawMaterialRequest request) {
        try {
            RawMaterialInventory material = stockService.addRawMaterial(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Raw material added successfully");
            response.put("data", material);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to add raw material: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Receive stock (Add stock)
    @PostMapping("/receive")
    public ResponseEntity<Map<String, Object>> receiveStock(@RequestBody StockMovementRequest request) {
        try {
            RawMaterialInventory material = stockService.receiveStock(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stock received successfully");
            response.put("currentStock", material.getCurrentStock());
            response.put("unit", material.getUnit());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to receive stock: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Issue stock (Consume/Use stock)
    @PostMapping("/issue")
    public ResponseEntity<Map<String, Object>> issueStock(@RequestBody StockMovementRequest request) {
        try {
            RawMaterialInventory material = stockService.issueStock(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stock issued successfully");
            response.put("remainingStock", material.getCurrentStock());
            response.put("unit", material.getUnit());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to issue stock: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Adjust stock (Corrections)
    @PostMapping("/adjust")
    public ResponseEntity<Map<String, Object>> adjustStock(@RequestBody StockMovementRequest request) {
        try {
            RawMaterialInventory material = stockService.adjustStock(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stock adjusted successfully");
            response.put("newStock", material.getCurrentStock());
            response.put("unit", material.getUnit());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to adjust stock: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Get movement history for a material
    @GetMapping("/movements/raw-material/{id}")
    public ResponseEntity<List<StockMovement>> getRawMaterialMovements(@PathVariable Long id) {
        return ResponseEntity.ok(stockService.getRawMaterialMovements(id));
    }

    // Get recent movements
    @GetMapping("/movements/recent")
    public ResponseEntity<List<StockMovement>> getRecentMovements(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(stockService.getRecentMovements(limit));
    }
}
