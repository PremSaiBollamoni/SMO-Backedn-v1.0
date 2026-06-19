package com.cutm.smo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ActiveJobDto {
    private Long jobId;
    private Long empId;
    private String empName;
    private Long wsId;
    private String wsCode;
    private String opName;
    private String skillGrade;
    private String barcode;
    private Integer bundleQty;
    private BigDecimal samValue;
    private BigDecimal estMinutes;
    private LocalDateTime startTime;
    private Long elapsedSeconds;
    private String status;
}
