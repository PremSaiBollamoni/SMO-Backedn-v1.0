package com.cutm.smo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterialStockDto {
    private Long rawMaterialId;
    private String materialType;
    private String materialName;
    private String materialCode;
    private BigDecimal currentStock;
    private String unit;
    private String warehouseLocation;
    private BigDecimal minStockLevel;
    private BigDecimal maxStockLevel;
    private BigDecimal reorderLevel;
    private String stockStatus; // LOW, NORMAL, HIGH, CRITICAL
    private LocalDateTime lastUpdated;
}
