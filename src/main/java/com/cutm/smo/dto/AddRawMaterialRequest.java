package com.cutm.smo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddRawMaterialRequest {
    private String materialType; // FABRIC, THREAD, BUTTON, ZIPPER, etc.
    private String materialName;
    private String materialCode;
    private BigDecimal initialStock;
    private String unit;
    private String warehouseLocation;
    private BigDecimal minStockLevel;
    private BigDecimal maxStockLevel;
    private BigDecimal reorderLevel;
    private Long vendorId;
    private BigDecimal unitPrice;
}
