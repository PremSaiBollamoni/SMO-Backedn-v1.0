package com.cutm.smo.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qr_scan_history")
public class QrScanHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "qr_id", nullable = false)
    private String qrId;
    
    @Column(name = "employee_id")
    private Long employeeId;
    
    @Column(name = "employee_name")
    private String employeeName;
    
    @Column(name = "scan_type", nullable = false)
    private String scanType; // CHECK_IN, CHECK_OUT
    
    @Column(name = "scan_time", nullable = false)
    private LocalDateTime scanTime;
    
    @Column(name = "scanned_by", nullable = false)
    private String scannedBy;
    
    @Column(name = "temp_qr_mapping_id")
    private Long tempQrMappingId;
    
    @PrePersist
    protected void onCreate() {
        if (scanTime == null) {
            scanTime = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getScanType() {
        return scanType;
    }
    
    public void setScanType(String scanType) {
        this.scanType = scanType;
    }
    
    public LocalDateTime getScanTime() {
        return scanTime;
    }
    
    public void setScanTime(LocalDateTime scanTime) {
        this.scanTime = scanTime;
    }
    
    public String getScannedBy() {
        return scannedBy;
    }
    
    public void setScannedBy(String scannedBy) {
        this.scannedBy = scannedBy;
    }
    
    public Long getTempQrMappingId() {
        return tempQrMappingId;
    }
    
    public void setTempQrMappingId(Long tempQrMappingId) {
        this.tempQrMappingId = tempQrMappingId;
    }
}
