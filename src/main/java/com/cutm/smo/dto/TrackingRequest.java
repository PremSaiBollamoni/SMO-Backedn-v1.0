package com.cutm.smo.dto;

import java.util.List;

public class TrackingRequest {
    private String machineQr;
    private String employeeQr;
    private List<Long> employeeIds;  // Multi-employee support (NEW)
    private String trayQr;
    private String status;
    private Long supervisorId; // Added for tracking who performed the action
    private Long operationId;  // Current operation being performed at this machine

    // Getters and Setters
    public String getMachineQr() {
        return machineQr;
    }

    public void setMachineQr(String machineQr) {
        this.machineQr = machineQr;
    }

    public String getEmployeeQr() {
        return employeeQr;
    }

    public void setEmployeeQr(String employeeQr) {
        this.employeeQr = employeeQr;
    }

    public List<Long> getEmployeeIds() {
        return employeeIds;
    }

    public void setEmployeeIds(List<Long> employeeIds) {
        this.employeeIds = employeeIds;
    }

    public String getTrayQr() {
        return trayQr;
    }

    public void setTrayQr(String trayQr) {
        this.trayQr = trayQr;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getSupervisorId() {
        return supervisorId;
    }

    public void setSupervisorId(Long supervisorId) {
        this.supervisorId = supervisorId;
    }

    public Long getOperationId() {
        return operationId;
    }

    public void setOperationId(Long operationId) {
        this.operationId = operationId;
    }
}
