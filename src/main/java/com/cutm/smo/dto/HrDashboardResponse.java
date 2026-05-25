package com.cutm.smo.dto;

public class HrDashboardResponse {
    private long totalRoles;
    private long totalEmployees;

    public HrDashboardResponse() {}

    public HrDashboardResponse(long totalRoles, long totalEmployees) {
        this.totalRoles = totalRoles;
        this.totalEmployees = totalEmployees;
    }

    public long getTotalRoles() {
        return totalRoles;
    }

    public void setTotalRoles(long totalRoles) {
        this.totalRoles = totalRoles;
    }

    public long getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(long totalEmployees) {
        this.totalEmployees = totalEmployees;
    }
}
