package com.cutm.smo.dto;

import java.util.List;
import java.util.Map;

public class LoginResponse {
    private String role;
    private String employeeName;
    private String empId;
    private String activities;
    /** JWT Access Token for API authentication */
    private String token;
    /** JWT Refresh Token for obtaining new access tokens */
    private String refreshToken;
    /** Token expiration time in milliseconds */
    private Long tokenExpiresIn;
    /** All roles assigned to this employee. Empty if only one role. */
    private List<Map<String, Object>> allRoles;

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

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getEmpId() { return empId; }
    public void setEmpId(String empId) { this.empId = empId; }
    public String getActivities() { return activities; }
    public void setActivities(String activities) { this.activities = activities; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public Long getTokenExpiresIn() { return tokenExpiresIn; }
    public void setTokenExpiresIn(Long tokenExpiresIn) { this.tokenExpiresIn = tokenExpiresIn; }
    public List<Map<String, Object>> getAllRoles() { return allRoles; }
    public void setAllRoles(List<Map<String, Object>> allRoles) { this.allRoles = allRoles; }
}
