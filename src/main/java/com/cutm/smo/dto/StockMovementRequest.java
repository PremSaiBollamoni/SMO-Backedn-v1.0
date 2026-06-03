package com.cutm.smo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementRequest {
    private String movementType; // RECEIPT, ISSUE, ADJUSTMENT, TRANSFER
    private String transactionType; // PURCHASE, PRODUCTION_USE, STOCK_CORRECTION, etc.
    private Long rawMaterialId; // For raw materials
    private Integer qty;
    private String unit;
    private String batchNumber;
    private Long operationId; // If issuing to an operation
    private Long performedBy; // Employee ID
    private String notes;
    private String reason; // For adjustments
}
