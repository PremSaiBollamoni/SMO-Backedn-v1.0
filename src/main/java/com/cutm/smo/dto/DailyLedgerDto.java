package com.cutm.smo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyLedgerDto {
    private Long ledgerId;
    private LocalDate ledgerDate;
    private Long operationId;
    private String operationName;
    private Integer openingStock;
    private Integer receivedQty;
    private Integer issuedQty;
    private Integer adjustedQty;
    private Integer closingStock;
    private String unit;
    private String stockStatus;
    private Integer minTarget;
    private Integer maxTarget;
}
