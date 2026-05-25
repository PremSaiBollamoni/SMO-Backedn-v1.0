package com.cutm.smo.dto;

public class TempQrScanRequest {
    private String qrId;
    private Long employeeId; // Only needed for check-in
    private String scannedBy;
    
    // Getters and Setters
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
    
    public String getScannedBy() {
        return scannedBy;
    }
    
    public void setScannedBy(String scannedBy) {
        this.scannedBy = scannedBy;
    }
}
