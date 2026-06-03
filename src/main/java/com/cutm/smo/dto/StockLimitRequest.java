package com.cutm.smo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockLimitRequest {
    private Long operationId;
    private Integer minQtyPerDay;
    private Integer maxQtyPerDay;
    private Integer minQtyPerMonth;
    private Integer maxQtyPerMonth;
    private Integer lowStockThreshold;
    private Integer highStockThreshold;
    private String unit;
}
