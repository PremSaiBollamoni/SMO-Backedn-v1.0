package com.cutm.smo.dto;

import java.time.LocalDateTime;

public class TempQrScanResponse {
    private String scanType; // CHECK_IN, CHECK_OUT
    private String qrId;
    private Long employeeId;
    private String employeeName;
    private LocalDateTime scanTime;
    private String message;
    private Long mappingId;
    
    // Getters and Setters
    public String getScanType() {
        return scanType;
    }
    
    public void setScanType(String scanType) {
        this.scanType = scanType;
    }
    
    public String getQrId() {
        return qrId;
    }
    
    public void setQrId(String qrId) {
        this.qrId = qrId;
    }
    
    public Long getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }
    
    public String getEmployeeName() {
        return employeeName;
    }
    
    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
    
    public LocalDateTime getScanTime() {
        return scanTime;
    }
    
    public void setScanTime(LocalDateTime scanTime) {
        this.scanTime = scanTime;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Long getMappingId() {
        return mappingId;
    }
    
    public void setMappingId(Long mappingId) {
        this.mappingId = mappingId;
    }
}
