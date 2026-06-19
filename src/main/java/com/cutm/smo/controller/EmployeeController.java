package com.cutm.smo.controller;

import com.cutm.smo.dto.CreateEmployeeRequest;
import com.cutm.smo.dto.EmployeeExportDto;
import com.cutm.smo.dto.HrDashboardResponse;
import com.cutm.smo.models.EmployeeInfo;
import com.cutm.smo.models.EmployeeLogin;
import com.cutm.smo.services.EmployeeExportService;
import com.cutm.smo.services.EmployeeService;
import com.cutm.smo.services.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final LoginService loginService;
    private final EmployeeExportService employeeExportService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('HR', 'MANAGER')")
    public HrDashboardResponse getDashboard() {
        return employeeService.getDashboard();
    }

    // ── Employees ────────────────────────────────────────────────────────────

    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR')")
    public List<EmployeeInfo> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @GetMapping("/employees/{id}")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR')")
    public EmployeeInfo getEmployeeById(@PathVariable Long id) {
        verifyEmployeeAccess(id);
        return employeeService.getEmployeeById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    @PostMapping("/employees")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR')")
    public EmployeeInfo createEmployee(
            @RequestParam(required = false, defaultValue = "system") String actorEmpId,
            @RequestBody CreateEmployeeRequest request) {
        return employeeService.createEmployee(actorEmpId, request);
    }

    @PutMapping("/employees/{id}")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR')")
    public EmployeeInfo updateEmployee(@PathVariable Long id, @RequestBody EmployeeInfo employee) {
        return employeeService.updateEmployee(id, employee);
    }

    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasAnyRole('HR')")
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
    }

    @DeleteMapping("/employees")
    @PreAuthorize("hasAnyRole('HR')")
    public void deleteEmployees(@RequestBody Map<String, List<String>> body) {
        List<String> ids = body.get("empIds");
        if (ids == null || ids.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empIds is required");
        employeeService.deleteEmployees(ids);
    }

    @GetMapping("/employees/export/data")
    @PreAuthorize("hasAnyRole('HR')")
    public List<EmployeeExportDto> exportEmployees() {
        return employeeExportService.getEmployeesForExport();
    }

    @GetMapping("/login/{empId}")
    @PreAuthorize("hasAnyRole('HR')")
    public EmployeeLogin getLogin(@PathVariable Long empId) {
        return loginService.getLoginById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Login not found"));
    }

    @PostMapping("/login")
    @PreAuthorize("hasAnyRole('HR')")
    public EmployeeLogin createLogin(@RequestBody EmployeeLogin login) {
        return loginService.createLogin(login);
    }

    @PutMapping("/login/{empId}")
    @PreAuthorize("hasAnyRole('HR')")
    public EmployeeLogin updateLogin(@PathVariable Long empId, @RequestBody EmployeeLogin login) {
        return loginService.updateLogin(empId, login);
    }

    /**
     * Verify that the current user has access to view/modify the employee.
     * IDOR Protection: Only HR or ADMIN can access other employees, others can only access themselves.
     */
    private void verifyEmployeeAccess(Long empId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long) {
            Long currentEmpId = (Long) principal;
            if (!currentEmpId.equals(empId) && !hasHRorAdminRole()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access this employee");
            }
        }
    }

    /**
     * Check if the current user has HR or ADMIN role.
     */
    private boolean hasHRorAdminRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR") || a.getAuthority().equals("ROLE_ADMIN"));
    }
}
