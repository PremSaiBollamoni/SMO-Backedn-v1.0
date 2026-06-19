package com.cutm.smo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class JobCompleteDto {
    private Long jobId;
    private String empName;
    private String opName;
    private Integer bundleQty;
    private BigDecimal samValue;
    private BigDecimal estMinutes;
    private BigDecimal actualMinutes;
    private BigDecimal efficiencyPct;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
