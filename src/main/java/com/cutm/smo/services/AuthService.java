package com.cutm.smo.services;

import com.cutm.smo.dto.LoginRequest;
import com.cutm.smo.dto.LoginResponse;
import com.cutm.smo.models.EmployeeInfo;
import com.cutm.smo.models.EmployeeLogin;
import com.cutm.smo.models.LoginAuditLog;
import com.cutm.smo.repositories.EmployeeInfoRepository;
import com.cutm.smo.repositories.EmployeeLoginRepository;
import com.cutm.smo.repositories.EmployeeRoleRepository;
import com.cutm.smo.repositories.LoginAuditLogRepository;
import com.cutm.smo.repositories.RoleRepository;
import com.cutm.smo.security.JwtTokenProvider;
import com.cutm.smo.security.PasswordUtil;
import com.cutm.smo.util.LoggingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
public class AuthService {
    private final EmployeeLoginRepository employeeLoginRepository;
    private final EmployeeInfoRepository employeeInfoRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final LoginAuditLogRepository loginAuditLogRepository;

    public AuthService(EmployeeLoginRepository employeeLoginRepository,
                       EmployeeInfoRepository employeeInfoRepository,
                       EmployeeRoleRepository employeeRoleRepository,
                       RoleRepository roleRepository,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder,
                       LoginAuditLogRepository loginAuditLogRepository) {
        this.employeeLoginRepository = employeeLoginRepository;
        this.employeeInfoRepository = employeeInfoRepository;
        this.employeeRoleRepository = employeeRoleRepository;
        this.roleRepository = roleRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.loginAuditLogRepository = loginAuditLogRepository;
    }

    public LoginResponse login(LoginRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("=== LOGIN PROCESS START ===");
            log.info("Login attempt for Employee ID: {}", request.getLoginid());
            
            // Validate input
            if (request.getLoginid() == null || request.getPassword() == null) {
                log.warn("Login validation failed: Missing loginid or password");
                LoggingUtil.logValidation(log, "LoginRequest", "loginid/password", false, "Missing required fields");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "loginid and password are required");
            }

            // Parse Employee ID
            Long empId = parseEmpId(request.getLoginid());
            log.debug("Parsed Employee ID: {}", empId);

            // Fetch login credentials from database
            log.debug("Querying database for login credentials with empId: {}", empId);
            EmployeeLogin login = employeeLoginRepository.findById(empId)
                    .orElseThrow(() -> {
                        log.warn("Login failed: Employee ID {} not found in login table", empId);
                        LoggingUtil.logAuthenticationAttempt(log, request.getLoginid(), false, "Employee not found");
                        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
                    });
            log.debug("Login record found for empId: {}", empId);

            // Check if user is active
            if (!"ACTIVE".equalsIgnoreCase(login.getStatus())) {
                log.warn("Login failed: User {} is not active. Status: {}", empId, login.getStatus());
                LoggingUtil.logAuthenticationAttempt(log, request.getLoginid(), false, "User is inactive");
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is inactive");
            }
            log.debug("User status verified: ACTIVE");

            // Verify password using BCrypt
            String storedPassword = login.getPassword();
            String providedPassword = request.getPassword().trim();

            boolean passwordMatches;
            if (PasswordUtil.isBcryptHashed(storedPassword)) {
                // Password is already hashed, use BCrypt matching
                passwordMatches = passwordEncoder.matches(providedPassword, storedPassword);
            } else {
                // Fallback for plain text passwords (for migration purposes)
                log.warn("Plain text password detected for empId: {}. This should be migrated to BCrypt.", empId);
                passwordMatches = storedPassword.equals(providedPassword);
            }

            if (!passwordMatches) {
                log.warn("Login failed: Invalid password for employee ID: {}", empId);
                LoggingUtil.logAuthenticationAttempt(log, request.getLoginid(), false, "Invalid password");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }
            log.debug("Password verified successfully");

            // Fetch employee information
            log.debug("Fetching employee information for empId: {}", empId);
            EmployeeInfo employeeInfo = employeeInfoRepository.findById(empId)
                    .orElseThrow(() -> {
                        log.error("Login failed: Employee info not found for empId: {}", empId);
                        LoggingUtil.logAuthenticationAttempt(log, request.getLoginid(), false, "Employee info not found");
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
                    });
            log.debug("Employee information retrieved: {}", employeeInfo.getEmpName());

            // Get role information — check employee_roles table first for multi-role support
            String roleName = employeeInfo.getRole() != null ? employeeInfo.getRole().getRoleName() : "USER";
            String activities = employeeInfo.getRole() != null ? employeeInfo.getRole().getActivity() : "";

            // Check if employee has multiple roles in employee_roles table
            java.util.List<com.cutm.smo.models.EmployeeRole> multiRoles =
                    employeeRoleRepository.findByEmpId(empId);

            java.util.List<java.util.Map<String, Object>> allRoles = new java.util.ArrayList<>();
            if (!multiRoles.isEmpty()) {
                // Build list of all roles with their activities
                for (com.cutm.smo.models.EmployeeRole er : multiRoles) {
                    roleRepository.findById(er.getRoleId()).ifPresent(role -> {
                        java.util.Map<String, Object> rm = new java.util.HashMap<>();
                        rm.put("roleId", role.getRoleId());
                        rm.put("roleName", role.getRoleName());
                        rm.put("activities", role.getActivity() != null ? role.getActivity() : "");
                        allRoles.add(rm);
                    });
                }
            }

            log.debug("User role: {}", roleName);
            log.debug("User activities: {}", activities);
            log.debug("Multi-roles count: {}", allRoles.size());

            // Generate JWT token
            String jwtToken = jwtTokenProvider.generateToken(
                    empId,
                    employeeInfo.getEmpName(),
                    roleName,
                    activities
            );
            String refreshToken = jwtTokenProvider.generateRefreshToken(empId);

            // Create response with JWT token
            LoginResponse response = new LoginResponse(roleName, employeeInfo.getEmpName(),
                    employeeInfo.getEmpId().toString(), activities);
            response.setToken(jwtToken);
            response.setRefreshToken(refreshToken);
            response.setTokenExpiresIn(jwtTokenProvider.getTokenExpirationMs());
            // Attach all roles so Flutter can show role picker if needed
            response.setAllRoles(allRoles);

            // Log successful login to database
            logSuccessfulLogin(empId);

            long endTime = System.currentTimeMillis();
            LoggingUtil.logAuthenticationAttempt(log, request.getLoginid(), true, null);
            LoggingUtil.logPerformance(log, "Authentication", startTime, endTime);
            log.info("=== LOGIN PROCESS END - SUCCESS ===");

            return response;
            
        } catch (ResponseStatusException e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Authentication (Failed)", startTime, endTime);
            log.error("=== LOGIN PROCESS END - FAILED ===");
            throw e;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Unexpected error during login for: " + request.getLoginid(), e);
            LoggingUtil.logPerformance(log, "Authentication (Error)", startTime, endTime);
            log.error("=== LOGIN PROCESS END - ERROR ===");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred during login");
        }
    }

    private Long parseEmpId(String value) {
        try {
            log.debug("Parsing Employee ID from string: {}", value);
            Long empId = Long.parseLong(value.trim());
            log.debug("Successfully parsed Employee ID: {}", empId);
            return empId;
        } catch (NumberFormatException ex) {
            log.warn("Failed to parse Employee ID: {}. Error: {}", value, ex.getMessage());
            LoggingUtil.logValidation(log, "EmployeeID", "format", false, "Invalid number format");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        } catch (Exception ex) {
            log.error("Unexpected error while parsing Employee ID: {}", value, ex);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    // Log successful login to database
    private void logSuccessfulLogin(Long empId) {
        try {
            HttpServletRequest request = getHttpServletRequest();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : "Unknown";

            LoginAuditLog auditLog = new LoginAuditLog(empId, ipAddress, userAgent);
            loginAuditLogRepository.save(auditLog);
            log.debug("Login audit logged for employee: {}", empId);
        } catch (Exception e) {
            log.warn("Failed to log login audit for employee: {}", empId, e);
            // Don't fail the login if audit logging fails
        }
    }

    // Log failed login to database
    public void logFailedLogin(Long empId, String failureReason, Integer attemptCount) {
        try {
            HttpServletRequest request = getHttpServletRequest();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : "Unknown";
            String status = attemptCount >= 5 ? "LOCKED" : "FAILED";

            LoginAuditLog auditLog = new LoginAuditLog(empId, status, failureReason, attemptCount, ipAddress, userAgent);
            loginAuditLogRepository.save(auditLog);
            log.debug("Failed login audit logged for employee: {} - Reason: {}", empId, failureReason);
        } catch (Exception e) {
            log.warn("Failed to log failed login audit for employee: {}", empId, e);
        }
    }

    // Helper method to get HttpServletRequest
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Helper method to get client IP address
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
