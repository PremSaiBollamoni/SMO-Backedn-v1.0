package com.cutm.smo.controller;

import com.cutm.smo.models.*;
import com.cutm.smo.dto.CreateEmployeeRequest;
import com.cutm.smo.services.*;
import com.cutm.smo.util.LoggingUtil;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/hr")
@CrossOrigin(origins = "*")
public class HrController {
    private final HrService hrService;
    private final AccessControlService accessControlService;

    public HrController(HrService hrService, AccessControlService accessControlService) {
        this.hrService = hrService;
        this.accessControlService = accessControlService;
    }

    @GetMapping("/roles")
    public List<Role> getAllRoles() {
        //long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET ALL ROLES START ===");
            //log.debug("Actor Employee ID: {}", actorEmpId);
            
            //accessControlService.require(actorEmpId, "ROLE_MANAGEMENT");
            //log.debug("Access control check passed for ROLE_MANAGEMENT");
            
            List<Role> roles = hrService.getAllRoles();
            log.info("Retrieved {} roles", roles.size());
            
            long endTime = System.currentTimeMillis();
            //LoggingUtil.logPerformance(log, "Get All Roles", startTime, endTime);
            log.info("=== GET ALL ROLES END - SUCCESS ===");
            return roles;
        } catch (Exception e) {
            //long endTime = System.currentTimeMillis();
            //LoggingUtil.logError(log, "Failed to get all roles for actor: " + actorEmpId, e);
            //LoggingUtil.logPerformance(log, "Get All Roles (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PostMapping("/roles")
    public Role createRole(@RequestParam String actorEmpId, @RequestBody Role role) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE ROLE START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Role Data: {}", role);
            
            accessControlService.require(actorEmpId, "ROLE_MANAGEMENT");
            log.debug("Access control check passed for ROLE_MANAGEMENT");
            
            Role createdRole = hrService.createRole(role);
            log.info("Role created successfully with ID: {}", createdRole.getRoleId());
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create Role", startTime, endTime);
            log.info("=== CREATE ROLE END - SUCCESS ===");
            return createdRole;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to create role for actor: " + actorEmpId, e);
            LoggingUtil.logPerformance(log, "Create Role (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping("/employees")
    public List<EmployeeInfo> getAllEmployees(@RequestParam String actorEmpId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET ALL EMPLOYEES START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            
            accessControlService.require(actorEmpId, "EMPLOYEE_MANAGEMENT");
            log.debug("Access control check passed for EMPLOYEE_MANAGEMENT");
            
            List<EmployeeInfo> employees = hrService.getAllEmployees();
            log.info("Retrieved {} employees", employees.size());
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get All Employees", startTime, endTime);
            log.info("=== GET ALL EMPLOYEES END - SUCCESS ===");
            return employees;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get all employees for actor: " + actorEmpId, e);
            LoggingUtil.logPerformance(log, "Get All Employees (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping("/employees/{id}")
    public EmployeeInfo getEmployeeById(@RequestParam String actorEmpId, @PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET EMPLOYEE BY ID START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Employee ID: {}", id);
            
            accessControlService.require(actorEmpId, "EMPLOYEE_MANAGEMENT");
            log.debug("Access control check passed for EMPLOYEE_MANAGEMENT");
            
            EmployeeInfo employee = hrService.getEmployeeById(id).orElse(null);
            if (employee != null) {
                log.info("Employee found: {}", employee.getEmpName());
            } else {
                log.warn("Employee not found with ID: {}", id);
            }
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Employee By ID", startTime, endTime);
            log.info("=== GET EMPLOYEE BY ID END - SUCCESS ===");
            return employee;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get employee with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Get Employee By ID (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PostMapping("/employees")
    public EmployeeInfo createEmployee(@RequestParam String actorEmpId, @RequestBody CreateEmployeeRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE EMPLOYEE START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Employee Request: {}", request);
            
            EmployeeInfo createdEmployee = hrService.createEmployee(actorEmpId, request);
            log.info("Employee created successfully with ID: {}", createdEmployee.getEmpId());
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create Employee", startTime, endTime);
            log.info("=== CREATE EMPLOYEE END - SUCCESS ===");
            return createdEmployee;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to create employee for actor: " + actorEmpId, e);
            LoggingUtil.logPerformance(log, "Create Employee (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PutMapping("/employees/{id}")
    public EmployeeInfo updateEmployee(
            @RequestParam String actorEmpId,
            @PathVariable Long id,
            @RequestBody EmployeeInfo employee) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== UPDATE EMPLOYEE START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Employee ID: {}", id);
            log.debug("Employee Data: {}", employee);
            
            accessControlService.require(actorEmpId, "EMPLOYEE_MANAGEMENT");
            log.debug("Access control check passed for EMPLOYEE_MANAGEMENT");
            
            EmployeeInfo updatedEmployee = hrService.updateEmployee(id, employee);
            log.info("Employee updated successfully with ID: {}", updatedEmployee.getEmpId());
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Update Employee", startTime, endTime);
            log.info("=== UPDATE EMPLOYEE END - SUCCESS ===");
            return updatedEmployee;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to update employee with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Update Employee (Failed)", startTime, endTime);
            throw e;
        }
    }

    @DeleteMapping("/employees/{id}")
    public void deleteEmployee(@RequestParam String actorEmpId, @PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== DELETE EMPLOYEE START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Employee ID to delete: {}", id);
            
            accessControlService.require(actorEmpId, "EMPLOYEE_MANAGEMENT");
            log.debug("Access control check passed for EMPLOYEE_MANAGEMENT");
            
            hrService.deleteEmployee(id);
            log.info("Employee deleted successfully with ID: {}", id);
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Delete Employee", startTime, endTime);
            log.info("=== DELETE EMPLOYEE END - SUCCESS ===");
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to delete employee with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Delete Employee (Failed)", startTime, endTime);
            throw e;
        }
    }

    @DeleteMapping("/employees")
    public void deleteEmployees(@RequestParam String actorEmpId, @RequestBody java.util.Map<String, List<String>> request) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== BULK DELETE EMPLOYEES START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            
            List<String> empIds = request.get("empIds");
            if (empIds == null || empIds.isEmpty()) {
                throw new IllegalArgumentException("empIds list is required and cannot be empty");
            }
            
            log.debug("Employee IDs to delete: {}", empIds);
            
            accessControlService.require(actorEmpId, "EMPLOYEE_MANAGEMENT");
            log.debug("Access control check passed for EMPLOYEE_MANAGEMENT");
            
            hrService.deleteEmployees(empIds);
            log.info("Bulk delete completed successfully for {} employees", empIds.size());
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Bulk Delete Employees", startTime, endTime);
            log.info("=== BULK DELETE EMPLOYEES END - SUCCESS ===");
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to bulk delete employees", e);
            LoggingUtil.logPerformance(log, "Bulk Delete Employees (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping("/employees/export/data")
    public java.util.List<com.cutm.smo.dto.EmployeeExportDto> exportEmployees(@RequestParam String actorEmpId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== EXPORT EMPLOYEES START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            
            java.util.List<com.cutm.smo.dto.EmployeeExportDto> data = hrService.getEmployeesForExport(actorEmpId);
            log.info("Exported {} employees", data.size());
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Export Employees", startTime, endTime);
            log.info("=== EXPORT EMPLOYEES END - SUCCESS ===");
            return data;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to export employees", e);
            LoggingUtil.logPerformance(log, "Export Employees (Failed)", startTime, endTime);
            throw e;
        }
    }

    @DeleteMapping("/roles/{id}")
    public void deleteRole(@RequestParam String actorEmpId, @PathVariable Integer id) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== DELETE ROLE START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Role ID to delete: {}", id);
            
            accessControlService.require(actorEmpId, "ROLE_MANAGEMENT");
            log.debug("Access control check passed for ROLE_MANAGEMENT");
            
            hrService.deleteRole(id.longValue());
            log.info("Role deleted successfully with ID: {}", id);
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Delete Role", startTime, endTime);
            log.info("=== DELETE ROLE END - SUCCESS ===");
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to delete role with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Delete Role (Failed)", startTime, endTime);
            throw e;
        }
    }

    @DeleteMapping("/roles")
    public void deleteRoles(@RequestParam String actorEmpId, @RequestBody java.util.Map<String, List<Integer>> request) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== BULK DELETE ROLES START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            
            List<Integer> roleIds = request.get("roleIds");
            if (roleIds == null || roleIds.isEmpty()) {
                throw new IllegalArgumentException("roleIds list is required and cannot be empty");
            }
            
            log.debug("Role IDs to delete: {}", roleIds);
            
            accessControlService.require(actorEmpId, "ROLE_MANAGEMENT");
            log.debug("Access control check passed for ROLE_MANAGEMENT");
            
            hrService.deleteRoles(roleIds);
            log.info("Bulk delete completed successfully for {} roles", roleIds.size());
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Bulk Delete Roles", startTime, endTime);
            log.info("=== BULK DELETE ROLES END - SUCCESS ===");
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to bulk delete roles", e);
            LoggingUtil.logPerformance(log, "Bulk Delete Roles (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping("/login/{empId}")
    public EmployeeLogin getLogin(@RequestParam String actorEmpId, @PathVariable Long empId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET LOGIN START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Employee ID: {}", empId);
            
            accessControlService.require(actorEmpId, "EMPLOYEE_MANAGEMENT");
            log.debug("Access control check passed for EMPLOYEE_MANAGEMENT");
            
            EmployeeLogin login = hrService.getLoginById(empId).orElse(null);
            if (login != null) {
                log.info("Login record found for employee: {}", empId);
            } else {
                log.warn("Login record not found for employee: {}", empId);
            }
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Login", startTime, endTime);
            log.info("=== GET LOGIN END - SUCCESS ===");
            return login;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get login for employee: " + empId, e);
            LoggingUtil.logPerformance(log, "Get Login (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PostMapping("/login")
    public EmployeeLogin createLogin(@RequestParam String actorEmpId, @RequestBody EmployeeLogin login) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE LOGIN START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Login Data: {}", login);
            
            accessControlService.require(actorEmpId, "EMPLOYEE_MANAGEMENT");
            log.debug("Access control check passed for EMPLOYEE_MANAGEMENT");
            
            EmployeeLogin createdLogin = hrService.createLogin(login);
            log.info("Login created successfully for employee: {}", createdLogin.getEmpId());
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create Login", startTime, endTime);
            log.info("=== CREATE LOGIN END - SUCCESS ===");
            return createdLogin;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to create login for actor: " + actorEmpId, e);
            LoggingUtil.logPerformance(log, "Create Login (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PutMapping("/login/{empId}")
    public EmployeeLogin updateLogin(
            @RequestParam String actorEmpId,
            @PathVariable Long empId,
            @RequestBody EmployeeLogin login) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== UPDATE LOGIN START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            log.debug("Employee ID: {}", empId);
            log.debug("Login Data: {}", login);
            
            accessControlService.require(actorEmpId, "EMPLOYEE_MANAGEMENT");
            log.debug("Access control check passed for EMPLOYEE_MANAGEMENT");
            
            EmployeeLogin updatedLogin = hrService.updateLogin(empId, login);
            log.info("Login updated successfully for employee: {}", updatedLogin.getEmpId());
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Update Login", startTime, endTime);
            log.info("=== UPDATE LOGIN END - SUCCESS ===");
            return updatedLogin;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to update login for employee: " + empId, e);
            LoggingUtil.logPerformance(log, "Update Login (Failed)", startTime, endTime);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Multi-role endpoints (additive — no existing endpoints modified)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/hr/employees/{empId}/roles
     * Returns all roles assigned to an employee (from employee_roles table).
     */
    @GetMapping("/employees/{empId}/roles")
    public List<java.util.Map<String, Object>> getEmployeeRoles(@PathVariable Long empId) {
        return hrService.getEmployeeRoles(empId);
    }

    /**
     * PUT /api/hr/employees/{empId}/roles
     * Replaces all roles for an employee.
     * Body: { "roleIds": [1, 2, 3] }
     */
    @PutMapping("/employees/{empId}/roles")
    public java.util.Map<String, Object> setEmployeeRoles(
            @PathVariable Long empId,
            @RequestBody java.util.Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> roleIds = (List<Integer>) body.get("roleIds");
        if (roleIds == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "roleIds is required");
        }
        List<Long> ids = roleIds.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList());
        hrService.setEmployeeRoles(empId, ids);
        return java.util.Map.of("success", true, "message", "Roles updated successfully");
    }

    // ─── Self-service Profile Endpoints (no activity check) ───

    /**
     * GET /api/hr/profile/{empId}
     * Any employee can view their own profile. No activity check required.
     */
    @GetMapping("/profile/{empId}")
    public EmployeeInfo getProfile(@PathVariable Long empId) {
        return hrService.getEmployeeById(empId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Employee not found"));
    }

    /**
     * PUT /api/hr/profile/{empId}
     * Any employee can update their own profile. No activity check required.
     */
    @PutMapping("/profile/{empId}")
    public java.util.Map<String, Object> updateProfile(
            @PathVariable Long empId,
            @RequestBody java.util.Map<String, Object> body) {
        EmployeeInfo emp = hrService.getEmployeeById(empId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Employee not found"));

        if (body.containsKey("empName")) emp.setEmpName((String) body.get("empName"));
        if (body.containsKey("email")) emp.setEmail((String) body.get("email"));
        if (body.containsKey("phone")) emp.setPhone((String) body.get("phone"));
        if (body.containsKey("address")) emp.setAddress((String) body.get("address"));
        if (body.containsKey("dob") && body.get("dob") != null) {
            emp.setDob(java.time.LocalDate.parse((String) body.get("dob")));
        }
        if (body.containsKey("bloodGroup")) emp.setBloodGroup((String) body.get("bloodGroup"));
        if (body.containsKey("emergencyContact")) emp.setEmergencyContact((String) body.get("emergencyContact"));
        if (body.containsKey("aadharNumber")) emp.setAadharNumber((String) body.get("aadharNumber"));
        if (body.containsKey("panCardNumber")) emp.setPanCardNumber((String) body.get("panCardNumber"));
        if (body.containsKey("status")) emp.setStatus((String) body.get("status"));

        hrService.saveEmployee(emp);

        // Update password if provided
        if (body.containsKey("password") && body.get("password") != null) {
            String newPassword = (String) body.get("password");
            if (!newPassword.trim().isEmpty()) {
                hrService.updatePassword(empId, newPassword);
            }
        }

        return java.util.Map.of("success", true, "message", "Profile updated successfully");
    }
}
