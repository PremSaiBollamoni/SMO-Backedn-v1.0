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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final LoginService loginService;
    private final EmployeeExportService employeeExportService;

    @GetMapping("/dashboard")
    public HrDashboardResponse getDashboard() {
        return employeeService.getDashboard();
    }

    // ── Employees ────────────────────────────────────────────────────────────

    @GetMapping("/employees")
    public List<EmployeeInfo> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @GetMapping("/employees/{id}")
    public EmployeeInfo getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    @PostMapping("/employees")
    public EmployeeInfo createEmployee(
            @RequestParam(required = false, defaultValue = "system") String actorEmpId,
            @RequestBody CreateEmployeeRequest request) {
        return employeeService.createEmployee(actorEmpId, request);
    }

    @PutMapping("/employees/{id}")
    public EmployeeInfo updateEmployee(@PathVariable Long id, @RequestBody EmployeeInfo employee) {
        return employeeService.updateEmployee(id, employee);
    }

    @DeleteMapping("/employees/{id}")
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
    }

    @DeleteMapping("/employees")
    public void deleteEmployees(@RequestBody Map<String, List<String>> body) {
        List<String> ids = body.get("empIds");
        if (ids == null || ids.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empIds is required");
        employeeService.deleteEmployees(ids);
    }

    @GetMapping("/employees/export/data")
    public List<EmployeeExportDto> exportEmployees() {
        return employeeExportService.getEmployeesForExport();
    }

    @GetMapping("/login/{empId}")
    public EmployeeLogin getLogin(@PathVariable Long empId) {
        return loginService.getLoginById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Login not found"));
    }

    @PostMapping("/login")
    public EmployeeLogin createLogin(@RequestBody EmployeeLogin login) {
        return loginService.createLogin(login);
    }

    @PutMapping("/login/{empId}")
    public EmployeeLogin updateLogin(@PathVariable Long empId, @RequestBody EmployeeLogin login) {
        return loginService.updateLogin(empId, login);
    }
}
