package com.cutm.smo.dto;

import lombok.Data;

@Data
public class AssignJobRequest {
    private String barcode;      // unique identifier (can be tray ID)
    private Long empId;
    private Long wsId;
    private Long assignedBy;
    private Integer bundleQty;   // tray quantity
}
