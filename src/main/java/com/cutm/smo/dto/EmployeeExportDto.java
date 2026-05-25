package com.cutm.smo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeExportDto {
    private String empId;
    private String empName;
    private String email;
    private String phone;
    private String address;
    private String dob;
    private String bloodGroup;
    private String emergencyContact;
    private String aadharNumber;
    private String panCardNumber;
    private String roleName;
    private String status;
    private String salary;
    private String empDate;
    private String loginStatus;
    private String createdByEmpId;
    private String createdByName;
    private String createdAt;
}
