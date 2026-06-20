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

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final EmployeeInfoRepository employeeInfoRepository;
    private final EmployeeLoginRepository employeeLoginRepository;

    public DataInitializer(
            RoleRepository roleRepository,
            EmployeeInfoRepository employeeInfoRepository,
            EmployeeLoginRepository employeeLoginRepository) {
        this.roleRepository = roleRepository;
        this.employeeInfoRepository = employeeInfoRepository;
        this.employeeLoginRepository = employeeLoginRepository;
    }

    @Override
    public void run(String... args) {

        Role hrRole = ensureRole("HR",
                "HR_DASHBOARD,HR_MANAGE_ROLES,HR_MANAGE_EMPLOYEES,HR_ATTENDANCE_REPORT,PROFILE_MANAGEMENT", "ACTIVE");

        Role supervisorRole = ensureRole("SUPERVISOR",
                "SUPERVISOR_WORK_ASSIGNMENT,SUPERVISOR_EFFICIENCY,SUPERVISOR_HISTORY,SUPERVISOR_ATTENDANCE,SUPERVISOR_LINE_BALANCING,PROFILE_MANAGEMENT", "ACTIVE");

        Role gmRole = ensureRole("GM",
                "GM_VIEW_PRODUCTION,GM_VIEW_REPORTS,GM_VIEW_EFFICIENCY,PROFILE_MANAGEMENT", "ACTIVE");

        Role processPlannerRole = ensureRole("Process Planner",
                "PROCESS_ROUTING,PROCESS_STAGES,MACHINE_ASSIGNMENT,PARALLEL_STEPS,MERGE_POINTS,STANDARD_TIME,WIP_LIMITS,PROFILE_MANAGEMENT", "ACTIVE");

        ensureEmployeeWithLogin(1001L, "Super", supervisorRole, "pass");
        ensureEmployeeWithLogin(1006L, "HR", hrRole, "pass");
    }

    private void ensureEmployeeWithLogin(Long empId, String empName, Role role, String password) {
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
