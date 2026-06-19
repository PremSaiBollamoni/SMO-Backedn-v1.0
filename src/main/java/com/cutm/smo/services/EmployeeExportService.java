package com.cutm.smo.services;

import com.cutm.smo.dto.EmployeeExportDto;
import com.cutm.smo.repositories.EmployeeInfoRepository;
import com.cutm.smo.repositories.EmployeeLoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeExportService {

    private final EmployeeInfoRepository employeeInfoRepository;
    private final EmployeeLoginRepository employeeLoginRepository;

    public List<EmployeeExportDto> getEmployeesForExport() {
        return employeeInfoRepository.findAll().stream().map(emp -> {
            EmployeeExportDto dto = new EmployeeExportDto();
            dto.setEmpId(emp.getEmpId().toString());
            dto.setEmpName(emp.getEmpName());
            dto.setEmail(emp.getEmail());
            dto.setPhone(emp.getPhone() != null ? emp.getPhone() : "");
            dto.setAddress(emp.getAddress() != null ? emp.getAddress() : "");
            dto.setDob(emp.getDob() != null ? emp.getDob().toString() : "");
            dto.setBloodGroup(emp.getBloodGroup() != null ? emp.getBloodGroup() : "");
            dto.setEmergencyContact(emp.getEmergencyContact() != null ? emp.getEmergencyContact() : "");
            dto.setAadharNumber(emp.getAadharNumber() != null ? emp.getAadharNumber() : "");
            dto.setPanCardNumber(emp.getPanCardNumber() != null ? emp.getPanCardNumber() : "");
            dto.setRoleName(emp.getRole() != null ? emp.getRole().getRoleName() : "");
            dto.setStatus(emp.getStatus());
            dto.setSalary(emp.getSalary() != null ? emp.getSalary().toString() : "0");
            dto.setEmpDate(emp.getEmpDate() != null ? emp.getEmpDate().toString() : "");
            employeeLoginRepository.findById(emp.getEmpId())
                    .ifPresent(l -> dto.setLoginStatus(l.getStatus()));
            if (emp.getCreatedBy() != null) {
                employeeInfoRepository.findById(emp.getCreatedBy()).ifPresent(creator -> {
                    dto.setCreatedByEmpId(creator.getEmpId().toString());
                    dto.setCreatedByName(creator.getEmpName());
                });
            }
            dto.setCreatedAt(emp.getCreatedAt() != null ? emp.getCreatedAt().toString() : "");
            return dto;
        }).toList();
    }
}
