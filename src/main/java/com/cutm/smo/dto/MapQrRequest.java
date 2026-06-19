package com.cutm.smo.dto;

import lombok.Data;

@Data
public class MapQrRequest {
    private String qrToken;   // e.g. EMP-TEMP-001
    private Long empId;
    private Long mappedBy;
}
