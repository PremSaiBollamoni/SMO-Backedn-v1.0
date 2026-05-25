package com.cutm.smo.dto;

public class LoginResponse {
    private String role;
    private String employeeName;
    private String empId;
    private String activities;

    public LoginResponse() {}

    public LoginResponse(String role, String employeeName, String empId) {
        this.role = role;
        this.employeeName = employeeName;
        this.empId = empId;
    }

    public LoginResponse(String role, String employeeName, String empId, String activities) {
        this.role = role;
        this.employeeName = employeeName;
        this.empId = empId;
        this.activities = activities;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public String getActivities() {
        return activities;
    }

    public void setActivities(String activities) {
        this.activities = activities;
    }
}
