package com.cutm.smo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationStockView {
    private Long operationId;
    private String operationName;
    private Integer sequence;
    private Integer binCount;
    private Integer actualQty;
    private Integer minTarget;
    private Integer maxTarget;
    private Integer varianceFromMin;
    private Integer spaceRemaining;
    private String stockStatus;
}
