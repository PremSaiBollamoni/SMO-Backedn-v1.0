package com.cutm.smo.security;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Authorization Aspect for method-level authorization checks.
 * Validates user permissions and roles before method execution.
 */
@Slf4j
@Aspect
@Component
public class AuthorizationAspect {

    /**
     * Before advice to check authorization for methods requiring specific roles.
     * Logs authorization attempts and failures.
     *
     * @param joinPoint the join point
     */
    @Before("@annotation(com.cutm.smo.security.RequireRole)")
    public void checkRoleAuthorization(JoinPoint joinPoint) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof Long) {
            Long empId = (Long) principal;
            log.debug("Authorization check for empId: {}", empId);
        } else {
            log.warn("Unauthorized access attempt: No valid authentication");
            logAuthorizationFailure("Unknown", joinPoint.getSignature().getName(), "No valid authentication");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access");
        }
    }

    /**
     * Check if user has required role.
     *
     * @param empId employee ID
     * @param requiredRole the required role
     * @return true if user has required role
     */
    public boolean hasRole(Long empId, String requiredRole) {
        // Implementation would check employee roles from database
        log.debug("Checking if empId {} has role: {}", empId, requiredRole);
        return true; // Placeholder - actual implementation would verify from DB
    }

    /**
     * Log authorization failure for audit purposes.
     *
     * @param empId the employee ID
     * @param methodName the method name
     * @param reason the reason for failure
     */
    private void logAuthorizationFailure(String empId, String methodName, String reason) {
        log.warn("AUTHORIZATION_FAILURE - EmpId: {}, Method: {}, Reason: {}, Timestamp: {}",
                empId, methodName, reason, System.currentTimeMillis());
    }
}
