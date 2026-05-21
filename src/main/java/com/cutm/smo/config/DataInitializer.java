package com.cutm.smo.config;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.cutm.smo.models.EmployeeInfo;
import com.cutm.smo.models.EmployeeLogin;
import com.cutm.smo.models.Role;
import com.cutm.smo.repositories.EmployeeInfoRepository;
import com.cutm.smo.repositories.EmployeeLoginRepository;
import com.cutm.smo.repositories.RoleRepository;
import com.cutm.smo.services.SensitiveDataService;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final EmployeeInfoRepository employeeInfoRepository;
    private final EmployeeLoginRepository employeeLoginRepository;
    private final SensitiveDataService sensitiveDataService;

    public DataInitializer(
            RoleRepository roleRepository,
            EmployeeInfoRepository employeeInfoRepository,
            EmployeeLoginRepository employeeLoginRepository,
            SensitiveDataService sensitiveDataService) {
        this.roleRepository = roleRepository;
        this.employeeInfoRepository = employeeInfoRepository;
        this.employeeLoginRepository = employeeLoginRepository;
        this.sensitiveDataService = sensitiveDataService;
    }

    @Override
    public void run(String... args) {

        // ── Only 4 Required Roles ───────────────────────────────────────────
        Role hrAdminRole = ensureRole("HR/Admin",
                "HR_DASHBOARD,ROLE_MANAGEMENT,EMPLOYEE_MANAGEMENT,PROFILE_MANAGEMENT", "ACTIVE");
        
        Role supervisorRole = ensureRole("Supervisor",
                "SUPERVISOR_DASHBOARD,SUPERVISOR_MONITORING,SUPERVISOR_REPORTS,PROFILE_MANAGEMENT", "ACTIVE");
        
        Role gmRole = ensureRole("General Manager (GM)",
                "GM_VIEW_PRODUCTION,GM_VIEW_INVENTORY_ANALYSIS,GM_VIEW_REPORTS,PP_APPROVE,PP_VIEW_ALL,PP_VIEW_NODE_METRICS,PROFILE_MANAGEMENT", "ACTIVE");
        
        Role processPlannerRole = ensureRole("Process Planner",
                "PROCESS_ROUTING,PROCESS_STAGES,MACHINE_ASSIGNMENT,PARALLEL_STEPS,MERGE_POINTS,STANDARD_TIME,WIP_LIMITS,PROFILE_MANAGEMENT", "ACTIVE");

        // ── Ensure employee and login records exist for 4 required employees ──
        ensureEmployeeWithLogin(1001L, "HR Admin", hrAdminRole, "hr123");
        ensureEmployeeWithLogin(1002L, "Supervisor One", supervisorRole, "supervisor123");
        ensureEmployeeWithLogin(1003L, "General Manager", gmRole, "gm123");
        ensureEmployeeWithLogin(1004L, "Process Planner", processPlannerRole, "planner123");

        // ── Encrypt sensitive fields if any unencrypted ────────────────────
        employeeInfoRepository.findAll().forEach(employee -> {
            boolean changed = false;
            String aadhar = employee.getAadharNumber();
            if (aadhar != null && !aadhar.isBlank() && !aadhar.contains(":")) {
                employee.setAadharNumber(sensitiveDataService.encrypt(aadhar));
                changed = true;
            }
            String pan = employee.getPanCardNumber();
            if (pan != null && !pan.isBlank() && !pan.contains(":")) {
                employee.setPanCardNumber(sensitiveDataService.encrypt(pan));
                changed = true;
            }
            if (changed) employeeInfoRepository.save(employee);
        });
    }

    private void ensureEmployeeWithLogin(Long empId, String empName, Role role, String password) {
        // Ensure employee record exists
        if (!employeeInfoRepository.existsById(empId)) {
            EmployeeInfo employee = new EmployeeInfo();
            employee.setEmpId(empId);
            employee.setEmpName(empName);
            employee.setRole(role);
            employee.setDob(LocalDate.of(1992, 1, 1));
            employee.setPhone("900000000" + empId);
            employee.setEmail(empName.toLowerCase().replace(" ", "") + "@smo.local");
            employee.setEmpDate(LocalDate.now());
            employee.setBloodGroup("O+");
            employee.setEmergencyContact("9000000099");
            employee.setStatus("ACTIVE");
            employeeInfoRepository.save(employee);
        }

        // Ensure login record exists
        if (!employeeLoginRepository.existsById(empId)) {
            EmployeeLogin login = new EmployeeLogin();
            login.setEmpId(empId);
            login.setPassword(password);
            login.setStatus("ACTIVE");
            employeeLoginRepository.save(login);
        }
    }

    private Role ensureRole(String roleName, String activity, String status) {
        return roleRepository.findByRoleNameIgnoreCase(roleName).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setRoleId(nextRoleId());
            newRole.setRoleName(roleName);
            newRole.setActivity(activity);
            newRole.setStatus(status);
            return roleRepository.save(newRole);
        });
    }

    private Long nextRoleId() {
        Long maxId = roleRepository.findMaxRoleId();
        return (maxId == null ? 0L : maxId) + 1L;
    }
}

