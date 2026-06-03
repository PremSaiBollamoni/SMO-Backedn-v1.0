package com.cutm.smo.services;

import com.cutm.smo.dto.AddRawMaterialRequest;
import com.cutm.smo.dto.RawMaterialStockDto;
import com.cutm.smo.dto.StockMovementRequest;
import com.cutm.smo.models.RawMaterialInventory;
import com.cutm.smo.models.StockMovement;
import com.cutm.smo.repository.RawMaterialInventoryRepository;
import com.cutm.smo.repository.StockMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockManagementService {

    @Autowired
    private RawMaterialInventoryRepository rawMaterialRepo;

    @Autowired
    private StockMovementRepository stockMovementRepo;

    // Get all raw materials with stock status
    public List<RawMaterialStockDto> getAllRawMaterials() {
        return rawMaterialRepo.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Get raw material by ID
    public RawMaterialStockDto getRawMaterialById(Long id) {
        return rawMaterialRepo.findById(id)
                .map(this::mapToDto)
                .orElse(null);
    }

    // Get materials by type
    public List<RawMaterialStockDto> getRawMaterialsByType(String type) {
        return rawMaterialRepo.findByMaterialType(type).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Get low stock materials
    public List<RawMaterialStockDto> getLowStockMaterials() {
        return rawMaterialRepo.findLowStockMaterials().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Add new raw material
    @Transactional
    public RawMaterialInventory addRawMaterial(AddRawMaterialRequest request) {
        RawMaterialInventory material = new RawMaterialInventory();
        material.setMaterialType(request.getMaterialType());
        material.setMaterialName(request.getMaterialName());
        material.setMaterialCode(request.getMaterialCode());
        material.setCurrentStock(request.getInitialStock() != null ? request.getInitialStock() : BigDecimal.ZERO);
        material.setUnit(request.getUnit());
        material.setWarehouseLocation(request.getWarehouseLocation());
        material.setMinStockLevel(request.getMinStockLevel());
        material.setMaxStockLevel(request.getMaxStockLevel());
        material.setReorderLevel(request.getReorderLevel());
        material.setVendorId(request.getVendorId());
        material.setUnitPrice(request.getUnitPrice());
        
        material = rawMaterialRepo.save(material);

        // Record initial stock as RECEIPT if > 0
        if (request.getInitialStock() != null && request.getInitialStock().compareTo(BigDecimal.ZERO) > 0) {
            StockMovement movement = new StockMovement();
            movement.setMovementType("RECEIPT");
            movement.setTransactionType("INITIAL_STOCK");
            movement.setItemType("RAW_MATERIAL");
            movement.setItemId(material.getRawMaterialId());
            movement.setQty(request.getInitialStock().intValue());
            movement.setUnit(request.getUnit());
            movement.setToLocationType("WAREHOUSE");
            movement.setNotes("Initial stock entry");
            stockMovementRepo.save(movement);
        }

        return material;
    }

    // Add stock (Receipt)
    @Transactional
    public RawMaterialInventory receiveStock(StockMovementRequest request) {
        RawMaterialInventory material = rawMaterialRepo.findById(request.getRawMaterialId())
                .orElseThrow(() -> new RuntimeException("Material not found"));

        // Update current stock
        BigDecimal newStock = material.getCurrentStock().add(BigDecimal.valueOf(request.getQty()));
        material.setCurrentStock(newStock);
        material.setLastPurchaseDate(LocalDate.now());
        material = rawMaterialRepo.save(material);

        // Record movement
        StockMovement movement = new StockMovement();
        movement.setMovementType("RECEIPT");
        movement.setTransactionType(request.getTransactionType() != null ? request.getTransactionType() : "PURCHASE");
        movement.setItemType("RAW_MATERIAL");
        movement.setItemId(request.getRawMaterialId());
        movement.setQty(request.getQty());
        movement.setUnit(request.getUnit());
        movement.setBatchNumber(request.getBatchNumber());
        movement.setToLocationType("WAREHOUSE");
        movement.setPerformedBy(request.getPerformedBy());
        movement.setNotes(request.getNotes());
        stockMovementRepo.save(movement);

        return material;
    }

    // Issue stock (Consumption)
    @Transactional
    public RawMaterialInventory issueStock(StockMovementRequest request) {
        RawMaterialInventory material = rawMaterialRepo.findById(request.getRawMaterialId())
                .orElseThrow(() -> new RuntimeException("Material not found"));

        // Check if sufficient stock
        BigDecimal requestedQty = BigDecimal.valueOf(request.getQty());
        if (material.getCurrentStock().compareTo(requestedQty) < 0) {
            throw new RuntimeException("Insufficient stock. Available: " + material.getCurrentStock() + " " + material.getUnit());
        }

        // Update current stock
        BigDecimal newStock = material.getCurrentStock().subtract(requestedQty);
        material.setCurrentStock(newStock);
        material = rawMaterialRepo.save(material);

        // Record movement
        StockMovement movement = new StockMovement();
        movement.setMovementType("ISSUE");
        movement.setTransactionType(request.getTransactionType() != null ? request.getTransactionType() : "PRODUCTION_USE");
        movement.setItemType("RAW_MATERIAL");
        movement.setItemId(request.getRawMaterialId());
        movement.setQty(request.getQty());
        movement.setUnit(request.getUnit());
        movement.setBatchNumber(request.getBatchNumber());
        movement.setFromLocationType("WAREHOUSE");
        movement.setToLocationType("OPERATION");
        movement.setToLocationId(request.getOperationId());
        movement.setOperationId(request.getOperationId());
        movement.setPerformedBy(request.getPerformedBy());
        movement.setNotes(request.getNotes());
        stockMovementRepo.save(movement);

        return material;
    }

    // Adjust stock (Corrections)
    @Transactional
    public RawMaterialInventory adjustStock(StockMovementRequest request) {
        RawMaterialInventory material = rawMaterialRepo.findById(request.getRawMaterialId())
                .orElseThrow(() -> new RuntimeException("Material not found"));

        // Update current stock (qty can be positive or negative)
        BigDecimal adjustment = BigDecimal.valueOf(request.getQty());
        BigDecimal newStock = material.getCurrentStock().add(adjustment);
        
        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Stock cannot be negative after adjustment");
        }

        material.setCurrentStock(newStock);
        material = rawMaterialRepo.save(material);

        // Record movement
        StockMovement movement = new StockMovement();
        movement.setMovementType("ADJUSTMENT");
        movement.setTransactionType("STOCK_CORRECTION");
        movement.setItemType("RAW_MATERIAL");
        movement.setItemId(request.getRawMaterialId());
        movement.setQty(request.getQty());
        movement.setUnit(request.getUnit());
        movement.setPerformedBy(request.getPerformedBy());
        movement.setNotes(request.getNotes() != null ? request.getNotes() : request.getReason());
        stockMovementRepo.save(movement);

        return material;
    }

    // Get stock movement history
    public List<StockMovement> getRawMaterialMovements(Long rawMaterialId) {
        return stockMovementRepo.findRawMaterialMovements(rawMaterialId);
    }

    // Get recent movements
    public List<StockMovement> getRecentMovements(int limit) {
        return stockMovementRepo.findAll().stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Helper method to map entity to DTO with stock status
    private RawMaterialStockDto mapToDto(RawMaterialInventory material) {
        RawMaterialStockDto dto = new RawMaterialStockDto();
        dto.setRawMaterialId(material.getRawMaterialId());
        dto.setMaterialType(material.getMaterialType());
        dto.setMaterialName(material.getMaterialName());
        dto.setMaterialCode(material.getMaterialCode());
        dto.setCurrentStock(material.getCurrentStock());
        dto.setUnit(material.getUnit());
        dto.setWarehouseLocation(material.getWarehouseLocation());
        dto.setMinStockLevel(material.getMinStockLevel());
        dto.setMaxStockLevel(material.getMaxStockLevel());
        dto.setReorderLevel(material.getReorderLevel());
        dto.setLastUpdated(material.getLastUpdated());

        // Determine stock status
        String status = "NORMAL";
        if (material.getCurrentStock().compareTo(material.getReorderLevel()) <= 0) {
            status = "CRITICAL";
        } else if (material.getCurrentStock().compareTo(material.getMinStockLevel()) <= 0) {
            status = "LOW";
        } else if (material.getMaxStockLevel().compareTo(BigDecimal.ZERO) > 0 
                   && material.getCurrentStock().compareTo(material.getMaxStockLevel()) >= 0) {
            status = "HIGH";
        }
        dto.setStockStatus(status);

        return dto;
    }
}
