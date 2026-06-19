package com.cutm.smo.dto;

import lombok.Data;

@Data
public class CheckInRequest {
    private String tempQrToken;   // e.g. EMP-TEMP-001
    private String machineCode;   // e.g. MACHINE-001
    private Long shiftTemplateId;
    private Long markedBy;        // supervisor emp_id
}
