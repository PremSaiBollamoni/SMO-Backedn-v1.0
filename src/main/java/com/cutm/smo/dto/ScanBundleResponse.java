package com.cutm.smo.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Returned when supervisor scans a bundle QR.
 * action: ASSIGN (first scan) | COMPLETE (second scan - job was IN_PROGRESS)
 */
@Data
public class ScanBundleResponse {
    private String action;           // ASSIGN | COMPLETE
    private Long jobId;
    private String barcode;
    private String wsCode;
    private String opName;
    private String skillGrade;
    private Integer bundleQty;
    private BigDecimal samValue;
    private BigDecimal estMinutes;
    private JobCompleteDto completedJob; // only set when action=COMPLETE
}
