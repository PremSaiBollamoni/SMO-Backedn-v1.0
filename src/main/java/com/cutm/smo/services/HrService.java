package com.cutm.smo.services;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.cutm.smo.util.LoggingUtil;
import lombok.extern.slf4j.Slf4j;

import com.cutm.smo.dto.CreateEmployeeRequest;
import com.cutm.smo.dto.CreateRoleRequest;
import com.cutm.smo.dto.EmployeeDto;
import com.cutm.smo.dto.EmployeeExportDto;
import com.cutm.smo.dto.HrDashboardResponse;
import com.cutm.smo.dto.HrProfileResponse;
import com.cutm.smo.dto.RoleDto;
import com.cutm.smo.dto.UpdateHrProfileRequest;
import com.cutm.smo.models.EmployeeInfo;
import com.cutm.smo.models.EmployeeLogin;
import com.cutm.smo.models.EmployeeRole;
import com.cutm.smo.models.Role;
import com.cutm.smo.repositories.EmployeeInfoRepository;
import com.cutm.smo.repositories.EmployeeLoginRepository;
import com.cutm.smo.repositories.EmployeeRoleRepository;
import com.cutm.smo.repositories.RoleRepository;

@Slf4j
@Service
public class HrService {
    private static final Set<String> ALLOWED_ROLE_STATUS = Set.of("ACTIVE", "INACTIVE");
    private static final Set<String> ALLOWED_EMPLOYEE_STATUS = Set.of("ACTIVE", "RESIGNED", "TERMINATED");

    private final RoleRepository roleRepository;
    private final EmployeeInfoRepository employeeInfoRepository;
    private final EmployeeLoginRepository employeeLoginRepository;
    private final SensitiveDataService sensitiveDataService;
    private final EmployeeRoleRepository employeeRoleRepository;

    public HrService(
            RoleRepository roleRepository,
            EmployeeInfoRepository employeeInfoRepository,
            EmployeeLoginRepository employeeLoginRepository,
            SensitiveDataService sensitiveDataService,
            EmployeeRoleRepository employeeRoleRepository) {
        this.roleRepository = roleRepository;
        this.employeeInfoRepository = employeeInfoRepository;
        this.employeeLoginRepository = employeeLoginRepository;
        this.sensitiveDataService = sensitiveDataService;
        this.employeeRoleRepository = employeeRoleRepository;
    }

    public HrDashboardResponse getDashboard(String actorEmpId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET HR DASHBOARD START ===");
            log.debug("Actor Employee ID: {}", actorEmpId);
            requireAccess(actorEmpId, "HR_DASHBOARD");
            long roleCount = roleRepository.count();
            long empCount = employeeInfoRepository.count();
            log.debug("Total Roles: {}, Total Employees: {}", roleCount, empCount);
            HrDashboardResponse response = new HrDashboardResponse(roleCount, empCount);
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get HR Dashboard", startTime, endTime);
            log.info("=== GET HR DASHBOARD END - SUCCESS ===");
            return response;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get HR dashboard", e);
            LoggingUtil.logPerformance(log, "Get HR Dashboard (Failed)", startTime, endTime);
            throw e;
        }
    }

    

    public List<EmployeeDto> getEmployees(String actorEmpId) {
        requireAccess(actorEmpId, "EMPLOYEE_MANAGEMENT");
        return employeeInfoRepository.findAll().stream().map(emp -> {
            EmployeeDto dto = new EmployeeDto();
            dto.setEmpId(emp.getEmpId().toString());
            dto.setEmpName(emp.getEmpName());
            dto.setRoleId(emp.getRole().getRoleId().toString());
            dto.setRoleName(emp.getRole().getRoleName());
            dto.setEmail(emp.getEmail());
            dto.setPhone(emp.getPhone());
            dto.setStatus(emp.getStatus());
            return dto;
        }).toList();
    }

    @Transactional
    public Role createRole(String actorEmpId, CreateRoleRequest request) {
        requireAccess(actorEmpId, "ROLE_MANAGEMENT");
        if (request.getRoleId() == null || request.getRoleId().isBlank()
                || request.getRoleName() == null || request.getRoleName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roleId and roleName are required");
        }
        Long roleId = parseRoleIdOrNext(request.getRoleId());
        if (roleRepository.existsById(roleId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role id already exists");
        }
        roleRepository.findByRoleNameIgnoreCase(request.getRoleName().trim()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists");
        });

        Role role = new Role();
        role.setRoleId(roleId);
        role.setRoleName(request.getRoleName().trim());
        role.setActivity(request.getActivity());
        role.setStatus(normalizeRoleStatus(request.getStatus()));
        return roleRepository.save(role);
    }

    @Transactional
    public EmployeeInfo createEmployee(String actorEmpId, CreateEmployeeRequest request) {
        requireAccess(actorEmpId, "EMPLOYEE_MANAGEMENT");
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
        employeeInfoRepository.findByEmailIgnoreCase(request.getEmail().trim()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        });

        Long roleId = parseRoleId(request.getRoleId());
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found"));
        if (!"ACTIVE".equalsIgnoreCase(role.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected role is inactive");
        }

        EmployeeInfo employee = new EmployeeInfo();
        employee.setEmpId(empId);
        employee.setEmpName(request.getEmpName().trim());
        employee.setRole(role);
        employee.setDob(request.getDob());
        employee.setPhone(request.getPhone());
        employee.setAddress(normalizeOptional(request.getAddress()));
        employee.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        employee.setSalary(request.getSalary());
        employee.setEmpDate(request.getEmpDate());
        employee.setBloodGroup(request.getBloodGroup());
        employee.setEmergencyContact(request.getEmergencyContact());
        employee.setAadharNumber(encryptIfPresent(request.getAadharNumber()));
        employee.setPanCardNumber(encryptIfPresent(request.getPanCardNumber()));
        employee.setStatus("ACTIVE");
        employee.setCreatedBy(parseEmpId(actorEmpId));
        employee.setCreatedAt(java.time.LocalDateTime.now());

        EmployeeInfo saved = employeeInfoRepository.save(employee);

        EmployeeLogin login = new EmployeeLogin();
        login.setEmpId(empId);
        login.setPassword(request.getPassword().trim());
        login.setStatus("ACTIVE");
        employeeLoginRepository.save(login);

        return saved;
    }

    public HrProfileResponse getProfile(String actorEmpId, String empId) {
        requireAccess(actorEmpId, "PROFILE_MANAGEMENT");
        Long actorId = parseEmpId(actorEmpId);
        Long targetId = parseEmpId(empId);
        // Non-HR roles can only view their own profile
        EmployeeInfo actor = employeeInfoRepository.findById(actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid actor"));
        boolean isHr = actor.getRole() != null &&
                (actor.getRole().getActivity().contains("HR_DASHBOARD") ||
                 actor.getRole().getActivity().equalsIgnoreCase("ADMIN"));
        if (!isHr && !actorId.equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own profile");
        }
        EmployeeInfo employee = employeeInfoRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        HrProfileResponse response = new HrProfileResponse();
        response.setEmpId(employee.getEmpId().toString());
        response.setEmpName(employee.getEmpName());
        response.setEmail(employee.getEmail());
        response.setPhone(employee.getPhone());
        response.setAddress(employee.getAddress());
        response.setDob(employee.getDob());
        response.setBloodGroup(employee.getBloodGroup());
        response.setEmergencyContact(employee.getEmergencyContact());
        response.setAadharNumber(maskSensitive(employee.getAadharNumber()));
        response.setPanCardNumber(maskSensitive(employee.getPanCardNumber()));
        response.setRoleName(employee.getRole().getRoleName());
        response.setStatus(employee.getStatus());
        return response;
    }

    @Transactional
    public HrProfileResponse updateProfile(String actorEmpId, String empId, UpdateHrProfileRequest request) {
        requireAccess(actorEmpId, "PROFILE_MANAGEMENT");
        Long numericEmpId = parseEmpId(empId);
        Long actorId = parseEmpId(actorEmpId);
        EmployeeInfo actor = employeeInfoRepository.findById(actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid actor"));
        boolean isHr = actor.getRole() != null &&
                (actor.getRole().getActivity().contains("HR_DASHBOARD") ||
                 actor.getRole().getActivity().equalsIgnoreCase("ADMIN"));
        if (!isHr && !actorId.equals(numericEmpId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own profile");
        }
        EmployeeInfo employee = employeeInfoRepository.findById(numericEmpId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        EmployeeLogin login = employeeLoginRepository.findById(numericEmpId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Login not found"));

        if (request.getEmpName() != null && !request.getEmpName().isBlank()) {
            employee.setEmpName(request.getEmpName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            employee.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        }
        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone().trim().isEmpty() ? null : request.getPhone().trim());
        }
        if (request.getAddress() != null) {
            employee.setAddress(normalizeOptional(request.getAddress()));
        }
        if (request.getDob() != null) {
            employee.setDob(request.getDob());
        }
        if (request.getBloodGroup() != null) {
            employee.setBloodGroup(request.getBloodGroup().trim().isEmpty() ? null : request.getBloodGroup().trim());
        }
        if (request.getEmergencyContact() != null) {
            employee.setEmergencyContact(
                    request.getEmergencyContact().trim().isEmpty() ? null : request.getEmergencyContact().trim());
        }
        if (request.getAadharNumber() != null) {
            if (!isMasked(request.getAadharNumber())) {
                employee.setAadharNumber(encryptIfPresent(request.getAadharNumber()));
            }
        }
        if (request.getPanCardNumber() != null) {
            if (!isMasked(request.getPanCardNumber())) {
                employee.setPanCardNumber(encryptIfPresent(request.getPanCardNumber()));
            }
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String status = normalizeEmployeeStatus(request.getStatus());
            employee.setStatus(status);
            login.setStatus(status);
        }
        employeeInfoRepository.save(employee);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            login.setPassword(request.getPassword().trim());
        }
        employeeLoginRepository.save(login);

        return getProfile(actorEmpId, numericEmpId.toString());
    }

    private String normalizeRoleStatus(String status) {
        String normalized = (status == null || status.isBlank())
                ? "ACTIVE"
                : status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLE_STATUS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role status must be ACTIVE or INACTIVE");
        }
        return normalized;
    }

    private String normalizeEmployeeStatus(String status) {
        String normalized = (status == null || status.isBlank())
                ? "ACTIVE"
                : status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_EMPLOYEE_STATUS.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Employee status must be ACTIVE, RESIGNED or TERMINATED");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String encryptIfPresent(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        return sensitiveDataService.encrypt(normalized);
    }

    private String maskSensitive(String encryptedValue) {
        String raw = sensitiveDataService.decrypt(encryptedValue);
        return sensitiveDataService.maskLast4(raw);
    }

    private boolean isMasked(String value) {
        return value != null && value.contains("*");
    }

    private void requireAccess(String actorEmpId, String requiredActivity) {
        if (actorEmpId == null || actorEmpId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing actor identity");
        }
        Long actorId = parseEmpId(actorEmpId);
        EmployeeInfo actor = employeeInfoRepository.findById(actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid actor"));
        EmployeeLogin login = employeeLoginRepository.findById(actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid actor login"));

        if (!"ACTIVE".equalsIgnoreCase(actor.getStatus()) || !"ACTIVE".equalsIgnoreCase(login.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is inactive");
        }
        Role role = actor.getRole();
        if (role == null || !"ACTIVE".equalsIgnoreCase(role.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is inactive");
        }
        String normalizedRequired = requiredActivity.trim().toUpperCase(Locale.ROOT);

        // Check primary role first
        String activity = role.getActivity() == null ? "" : role.getActivity();
        boolean allowed = activity.equalsIgnoreCase("ALL")
                || activity.equalsIgnoreCase("ADMIN")
                || java.util.Arrays.stream(activity.split(","))
                        .map(a -> a.trim().toUpperCase(Locale.ROOT))
                        .anyMatch(a -> a.equals(normalizedRequired));

        // If primary role doesn't have the activity, check all assigned roles
        // (supports multi-role employees who switched roles mid-session)
        if (!allowed) {
            java.util.List<com.cutm.smo.models.EmployeeRole> multiRoles =
                    employeeRoleRepository.findByEmpId(actorId);
            for (com.cutm.smo.models.EmployeeRole er : multiRoles) {
                java.util.Optional<Role> extraRole = roleRepository.findById(er.getRoleId());
                if (extraRole.isPresent() && "ACTIVE".equalsIgnoreCase(extraRole.get().getStatus())) {
                    String extraActivity = extraRole.get().getActivity() == null ? "" : extraRole.get().getActivity();
                    boolean extraAllowed = extraActivity.equalsIgnoreCase("ALL")
                            || extraActivity.equalsIgnoreCase("ADMIN")
                            || java.util.Arrays.stream(extraActivity.split(","))
                                    .map(a -> a.trim().toUpperCase(Locale.ROOT))
                                    .anyMatch(a -> a.equals(normalizedRequired));
                    if (extraAllowed) {
                        allowed = true;
                        break;
                    }
                }
            }
        }

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for activity " + requiredActivity);
        }
    }

    private Long parseEmpId(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empId must be numeric");
        }
    }

    private Long parseEmpIdOrNext(String value) {
        if (value == null || value.isBlank()) {
            return employeeInfoRepository.findMaxEmpId() + 1;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ex) {
            return employeeInfoRepository.findMaxEmpId() + 1;
        }
    }

    private Long parseRoleId(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roleId must be numeric");
        }
    }

    private Long parseRoleIdOrNext(String value) {
        if (value == null || value.isBlank()) {
            return roleRepository.findMaxRoleId() + 1;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ex) {
            return roleRepository.findMaxRoleId() + 1;
        }
    }

    public Role createRole(Role role) { return roleRepository.save(role); }

    public List<Role> getAllRoles() { return roleRepository.findAll(); }

    public List<EmployeeInfo> getAllEmployees() { return employeeInfoRepository.findAll(); }

    public java.util.Optional<EmployeeInfo> getEmployeeById(Long id) { return employeeInfoRepository.findById(id); }

    public EmployeeInfo createEmployee(EmployeeInfo employee) { return employeeInfoRepository.save(employee); }

    public EmployeeInfo updateEmployee(Long id, EmployeeInfo employee) { 
        EmployeeInfo existing = employeeInfoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        
        // Only update fields that are provided (not null)
        if (employee.getEmpName() != null && !employee.getEmpName().isBlank()) {
            existing.setEmpName(employee.getEmpName().trim());
        }
        if (employee.getEmail() != null && !employee.getEmail().isBlank()) {
            existing.setEmail(employee.getEmail().trim().toLowerCase(Locale.ROOT));
        }
        if (employee.getPhone() != null) {
            existing.setPhone(employee.getPhone().trim().isEmpty() ? null : employee.getPhone().trim());
        }
        if (employee.getAddress() != null) {
            existing.setAddress(normalizeOptional(employee.getAddress()));
        }
        if (employee.getDob() != null) {
            existing.setDob(employee.getDob());
        }
        if (employee.getBloodGroup() != null) {
            existing.setBloodGroup(employee.getBloodGroup().trim().isEmpty() ? null : employee.getBloodGroup().trim());
        }
        if (employee.getEmergencyContact() != null) {
            existing.setEmergencyContact(employee.getEmergencyContact().trim().isEmpty() ? null : employee.getEmergencyContact().trim());
        }
        if (employee.getAadharNumber() != null && !isMasked(employee.getAadharNumber())) {
            existing.setAadharNumber(encryptIfPresent(employee.getAadharNumber()));
        }
        if (employee.getPanCardNumber() != null && !isMasked(employee.getPanCardNumber())) {
            existing.setPanCardNumber(encryptIfPresent(employee.getPanCardNumber()));
        }
        if (employee.getRole() != null) {
            existing.setRole(employee.getRole());
        }
        if (employee.getStatus() != null && !employee.getStatus().isBlank()) {
            existing.setStatus(normalizeEmployeeStatus(employee.getStatus()));
        }
        if (employee.getSalary() != null) {
            existing.setSalary(employee.getSalary());
        }
        if (employee.getEmpDate() != null) {
            existing.setEmpDate(employee.getEmpDate());
        }
        
        return employeeInfoRepository.save(existing);
    }

    public void deleteEmployee(Long id) { 
        employeeInfoRepository.deleteById(id); 
        // Also delete the login record
        employeeLoginRepository.deleteById(id);
    }

    @Transactional
    public void deleteEmployees(List<String> empIds) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== DELETE EMPLOYEES SERVICE START ===");
            log.debug("Employee IDs to delete: {}", empIds);
            
            int successCount = 0;
            int failCount = 0;
            
            for (String empIdStr : empIds) {
                try {
                    Long empId = parseEmpId(empIdStr);
                    employeeInfoRepository.deleteById(empId);
                    employeeLoginRepository.deleteById(empId);
                    successCount++;
                    log.debug("Deleted employee: {}", empId);
                } catch (Exception e) {
                    failCount++;
                    log.error("Failed to delete employee: {}", empIdStr, e);
                }
            }
            
            log.info("Bulk delete completed: {} succeeded, {} failed", successCount, failCount);
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Delete Employees Service", startTime, endTime);
            log.info("=== DELETE EMPLOYEES SERVICE END ===");
            
            if (failCount > 0 && successCount == 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to delete all employees");
            }
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Error in bulk delete employees", e);
            LoggingUtil.logPerformance(log, "Delete Employees Service (Failed)", startTime, endTime);
            throw e;
        }
    }

    public void deleteRole(Long id) {
        // Check if any employees are using this role
        List<EmployeeInfo> employeesWithRole = employeeInfoRepository.findByRoleRoleId(id);
        if (!employeesWithRole.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "Cannot delete role: " + employeesWithRole.size() + " employee(s) are assigned to this role");
        }
        roleRepository.deleteById(id);
    }

    @Transactional
    public void deleteRoles(List<Integer> roleIds) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== DELETE ROLES SERVICE START ===");
            log.debug("Role IDs to delete: {}", roleIds);
            
            int successCount = 0;
            int failCount = 0;
            
            for (Integer roleId : roleIds) {
                try {
                    Long roleIdLong = roleId.longValue();
                    
                    // Check if any employees are using this role
                    List<EmployeeInfo> employeesWithRole = employeeInfoRepository.findByRoleRoleId(roleIdLong);
                    if (!employeesWithRole.isEmpty()) {
                        log.warn("Cannot delete role {}: {} employee(s) assigned", roleId, employeesWithRole.size());
                        failCount++;
                        continue;
                    }
                    
                    roleRepository.deleteById(roleIdLong);
                    successCount++;
                    log.debug("Deleted role: {}", roleId);
                } catch (Exception e) {
                    failCount++;
                    log.error("Failed to delete role: {}", roleId, e);
                }
            }
            
            log.info("Bulk delete completed: {} succeeded, {} failed", successCount, failCount);
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Delete Roles Service", startTime, endTime);
            log.info("=== DELETE ROLES SERVICE END ===");
            
            if (failCount > 0 && successCount == 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to delete all roles");
            }
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Error in bulk delete roles", e);
            LoggingUtil.logPerformance(log, "Delete Roles Service (Failed)", startTime, endTime);
            throw e;
        }
    }

    public java.util.Optional<EmployeeLogin> getLoginById(Long empId) { return employeeLoginRepository.findById(empId); }

    public java.util.List<EmployeeExportDto> getEmployeesForExport(String actorEmpId) {
        requireAccess(actorEmpId, "EMPLOYEE_MANAGEMENT");
        
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
            dto.setAadharNumber(maskSensitive(emp.getAadharNumber()));
            dto.setPanCardNumber(maskSensitive(emp.getPanCardNumber()));
            dto.setRoleName(emp.getRole() != null ? emp.getRole().getRoleName() : "");
            dto.setStatus(emp.getStatus());
            dto.setSalary(emp.getSalary() != null ? emp.getSalary().toString() : "0");
            dto.setEmpDate(emp.getEmpDate() != null ? emp.getEmpDate().toString() : "");
            
            // Get login status
            employeeLoginRepository.findById(emp.getEmpId()).ifPresent(login -> {
                dto.setLoginStatus(login.getStatus());
            });
            
            // Get creator info
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

    public EmployeeLogin createLogin(EmployeeLogin login) {
        if (login == null || login.getEmpId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empId is required");
        }
        if (!employeeInfoRepository.existsById(login.getEmpId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        }
        if (employeeLoginRepository.existsById(login.getEmpId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Login already exists for employee");
        }
        if (login.getPassword() == null || login.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is required");
        }
        if (login.getStatus() == null || login.getStatus().isBlank()) {
            login.setStatus("ACTIVE");
        }
        return employeeLoginRepository.save(login);
    }

    public EmployeeLogin updateLogin(Long empId, EmployeeLogin login) {
        if (empId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empId is required");
        }
        if (!employeeInfoRepository.existsById(empId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        }
        EmployeeLogin existing = employeeLoginRepository.findById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Login not found"));
        if (login != null && login.getPassword() != null && !login.getPassword().isBlank()) {
            existing.setPassword(login.getPassword().trim());
        }
        if (login != null && login.getStatus() != null && !login.getStatus().isBlank()) {
            existing.setStatus(login.getStatus().trim().toUpperCase(Locale.ROOT));
        }
        return employeeLoginRepository.save(existing);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Multi-role support (additive)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Get all roles assigned to an employee from the employee_roles table.
     * Returns a list of maps with roleId, roleName, activities, status.
     */
    public List<java.util.Map<String, Object>> getEmployeeRoles(Long empId) {
        List<EmployeeRole> mappings = employeeRoleRepository.findByEmpId(empId);
        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (EmployeeRole mapping : mappings) {
            roleRepository.findById(mapping.getRoleId()).ifPresent(role -> {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("roleId", role.getRoleId());
                m.put("roleName", role.getRoleName());
                m.put("activities", role.getActivity());
                m.put("status", role.getStatus());
                result.add(m);
            });
        }
        return result;
    }

    /**
     * Replace all roles for an employee.
     * Also updates employee.role_id to the first role in the list (primary role).
     */
    @Transactional
    public void setEmployeeRoles(Long empId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one role is required");
        }
        // Validate all role IDs exist
        for (Long roleId : roleIds) {
            if (!roleRepository.existsById(roleId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found: " + roleId);
            }
        }
        // Validate employee exists
        EmployeeInfo emp = employeeInfoRepository.findById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        // Replace all mappings
        employeeRoleRepository.deleteByEmpId(empId);
        for (Long roleId : roleIds) {
            EmployeeRole er = new EmployeeRole();
            er.setEmpId(empId);
            er.setRoleId(roleId);
            employeeRoleRepository.save(er);
        }

        // Update primary role on employee to first in list
        Role primaryRole = roleRepository.findById(roleIds.get(0)).orElse(null);
        if (primaryRole != null) {
            emp.setRole(primaryRole);
            employeeInfoRepository.save(emp);
        }
        log.info("[setEmployeeRoles] Updated roles for empId={}: {}", empId, roleIds);
    }

    /**
     * Get all roles for an employee as a combined activities string.
     * Used by login to return merged activities when employee has multiple roles.
     */
    public String getMergedActivities(Long empId) {
        List<EmployeeRole> mappings = employeeRoleRepository.findByEmpId(empId);
        if (mappings.isEmpty()) return null;
        java.util.Set<String> allActivities = new java.util.LinkedHashSet<>();
        for (EmployeeRole mapping : mappings) {
            roleRepository.findById(mapping.getRoleId()).ifPresent(role -> {
                if (role.getActivity() != null && !role.getActivity().trim().isEmpty()) {
                    for (String act : role.getActivity().split(",")) {
                        allActivities.add(act.trim());
                    }
                }
            });
        }
        return allActivities.isEmpty() ? null : String.join(",", allActivities);
    }
}
