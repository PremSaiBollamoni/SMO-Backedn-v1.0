package com.cutm.smo.services;

import com.cutm.smo.dto.LoginRequest;
import com.cutm.smo.dto.LoginResponse;
import com.cutm.smo.models.EmployeeInfo;
import com.cutm.smo.models.EmployeeLogin;
import com.cutm.smo.repositories.EmployeeInfoRepository;
import com.cutm.smo.repositories.EmployeeLoginRepository;
import com.cutm.smo.util.LoggingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class AuthService {
    private final EmployeeLoginRepository employeeLoginRepository;
    private final EmployeeInfoRepository employeeInfoRepository;

    public AuthService(EmployeeLoginRepository employeeLoginRepository, EmployeeInfoRepository employeeInfoRepository) {
        this.employeeLoginRepository = employeeLoginRepository;
        this.employeeInfoRepository = employeeInfoRepository;
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

            // Verify password
            if (!login.getPassword().equals(request.getPassword().trim())) {
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

            // Get role information
            String roleName = employeeInfo.getRole() != null ? employeeInfo.getRole().getRoleName() : "USER";
            String activities = employeeInfo.getRole() != null ? employeeInfo.getRole().getActivity() : "";
            log.debug("User role: {}", roleName);
            log.debug("User activities: {}", activities);

            // Create response
            LoginResponse response = new LoginResponse(roleName, employeeInfo.getEmpName(), employeeInfo.getEmpId().toString(), activities);
            
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
}
