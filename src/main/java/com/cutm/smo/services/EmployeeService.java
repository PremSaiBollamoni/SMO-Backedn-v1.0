package com.cutm.smo.services;

import com.cutm.smo.dto.CreateEmployeeRequest;
import com.cutm.smo.dto.HrDashboardResponse;
import com.cutm.smo.models.EmployeeInfo;
import com.cutm.smo.models.EmployeeLogin;
import com.cutm.smo.models.Role;
import com.cutm.smo.repositories.EmployeeInfoRepository;
import com.cutm.smo.repositories.EmployeeLoginRepository;
import com.cutm.smo.repositories.RoleRepository;
import com.cutm.smo.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeInfoRepository employeeInfoRepository;
    private final EmployeeLoginRepository employeeLoginRepository;
    private final RoleRepository roleRepository;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    public HrDashboardResponse getDashboard() {
        return new HrDashboardResponse(roleRepository.count(), employeeInfoRepository.count());
    }

    // ── Employee CRUD ─────────────────────────────────────────────────────────

    public List<EmployeeInfo> getAllEmployees() {
        return employeeInfoRepository.findAll();
    }

    public Optional<EmployeeInfo> getEmployeeById(Long id) {
        return employeeInfoRepository.findById(id);
    }

    @Transactional
    public EmployeeInfo createEmployee(String actorEmpId, CreateEmployeeRequest request) {
        if (request.getEmpId() == null || request.getEmpId().isBlank()
                || request.getEmpName() == null || request.getEmpName().isBlank()
                || request.getRoleId() == null || request.getRoleId().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getEmpDate() == null
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "empId, empName, roleId, email, empDate and password are required");
        }
        Long empId = parseEmpIdOrNext(request.getEmpId());
        if (employeeInfoRepository.existsById(empId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee id already exists");
        }
        employeeInfoRepository.findByEmailIgnoreCase(request.getEmail().trim()).ifPresent(e -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        });
        Long roleId = parseId(request.getRoleId(), "roleId");
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found"));
        if (!"ACTIVE".equalsIgnoreCase(role.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected role is inactive");
        }
        EmployeeInfo emp = new EmployeeInfo();
        emp.setEmpId(empId);
        emp.setEmpName(request.getEmpName().trim());
        emp.setRole(role);
        emp.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        emp.setSalary(request.getSalary());
        emp.setEmpDate(request.getEmpDate());
        emp.setBloodGroup(request.getBloodGroup());
        emp.setEmergencyContact(request.getEmergencyContact());
        emp.setAadharNumber(normalizeOptional(request.getAadharNumber()));
        emp.setPanCardNumber(normalizeOptional(request.getPanCardNumber()));
        emp.setStatus("ACTIVE");
        emp.setCreatedBy(parseId(actorEmpId, "actorEmpId"));
        EmployeeInfo saved = employeeInfoRepository.save(emp);

        EmployeeLogin login = new EmployeeLogin();
        login.setEmpId(empId);
        login.setPassword(request.getPassword().trim());
        login.setStatus("ACTIVE");
        employeeLoginRepository.save(login);
        return saved;
    }

    public EmployeeInfo saveEmployee(EmployeeInfo employee) {
        return employeeInfoRepository.save(employee);
    }

    @Transactional
    public EmployeeInfo updateEmployee(Long id, EmployeeInfo patch) {
        EmployeeInfo existing = employeeInfoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        if (patch.getEmpName() != null && !patch.getEmpName().isBlank())
            existing.setEmpName(patch.getEmpName().trim());
        if (patch.getEmail() != null && !patch.getEmail().isBlank())
            existing.setEmail(patch.getEmail().trim().toLowerCase(Locale.ROOT));
        if (patch.getPhone() != null)
            existing.setPhone(patch.getPhone().trim().isEmpty() ? null : patch.getPhone().trim());
        if (patch.getAddress() != null)
            existing.setAddress(normalizeOptional(patch.getAddress()));
        if (patch.getDob() != null) existing.setDob(patch.getDob());
        if (patch.getBloodGroup() != null)
            existing.setBloodGroup(patch.getBloodGroup().trim().isEmpty() ? null : patch.getBloodGroup().trim());
        if (patch.getEmergencyContact() != null)
            existing.setEmergencyContact(
                    patch.getEmergencyContact().trim().isEmpty() ? null : patch.getEmergencyContact().trim());
        if (patch.getAadharNumber() != null)
            existing.setAadharNumber(normalizeOptional(patch.getAadharNumber()));
        if (patch.getPanCardNumber() != null)
            existing.setPanCardNumber(normalizeOptional(patch.getPanCardNumber()));
        if (patch.getRole() != null) existing.setRole(patch.getRole());
        if (patch.getStatus() != null && !patch.getStatus().isBlank())
            existing.setStatus(normalizeEmployeeStatus(patch.getStatus()));
        if (patch.getSalary() != null) existing.setSalary(patch.getSalary());
        if (patch.getEmpDate() != null) existing.setEmpDate(patch.getEmpDate());
        return employeeInfoRepository.save(existing);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        employeeInfoRepository.deleteById(id);
        employeeLoginRepository.deleteById(id);
    }

    @Transactional
    public void deleteEmployees(List<String> empIds) {
        long start = System.currentTimeMillis();
        int ok = 0, fail = 0;
        for (String idStr : empIds) {
            try {
                Long id = parseId(idStr, "empId");
                employeeInfoRepository.deleteById(id);
                employeeLoginRepository.deleteById(id);
                ok++;
            } catch (Exception e) {
                log.error("Failed to delete employee {}", idStr, e);
                fail++;
            }
        }
        LoggingUtil.logPerformance(log, "deleteEmployees", start, System.currentTimeMillis());
        if (ok == 0 && fail > 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete all employees");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Long parseId(String value, String fieldName) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be numeric");
        }
    }

    private Long parseEmpIdOrNext(String value) {
        if (value == null || value.isBlank()) return employeeInfoRepository.findMaxEmpId() + 1;
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return employeeInfoRepository.findMaxEmpId() + 1;
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private String normalizeEmployeeStatus(String status) {
        String s = status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVE", "RESIGNED", "TERMINATED").contains(s)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Employee status must be ACTIVE, RESIGNED or TERMINATED");
        }
        return s;
    }
}
