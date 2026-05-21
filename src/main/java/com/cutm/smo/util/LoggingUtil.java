package com.cutm.smo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Centralized logging utility for consistent logging across the application
 */
public class LoggingUtil {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Log API request details
     */
    public static void logApiRequest(Logger log, String endpoint, String method, Object requestBody, String actorEmpId) {
        log.info("=== API REQUEST START ===");
        log.info("Endpoint: {} {}", method, endpoint);
        log.info("Actor Employee ID: {}", actorEmpId);
        if (requestBody != null) {
            log.info("Request Body: {}", gson.toJson(requestBody));
        }
        log.info("Timestamp: {}", System.currentTimeMillis());
    }

    /**
     * Log API response details
     */
    public static void logApiResponse(Logger log, String endpoint, int statusCode, Object responseBody) {
        log.info("=== API RESPONSE ===");
        log.info("Endpoint: {}", endpoint);
        log.info("Status Code: {}", statusCode);
        if (responseBody != null) {
            log.info("Response Body: {}", gson.toJson(responseBody));
        }
        log.info("Timestamp: {}", System.currentTimeMillis());
        log.info("=== API REQUEST END ===");
    }

    /**
     * Log database query execution
     */
    public static void logDatabaseQuery(Logger log, String operation, String entity, Object data) {
        log.debug("=== DATABASE OPERATION ===");
        log.debug("Operation: {}", operation);
        log.debug("Entity: {}", entity);
        if (data != null) {
            log.debug("Data: {}", gson.toJson(data));
        }
        log.debug("Timestamp: {}", System.currentTimeMillis());
    }

    /**
     * Log database query result
     */
    public static void logDatabaseResult(Logger log, String operation, String entity, Object result, long executionTime) {
        log.debug("=== DATABASE RESULT ===");
        log.debug("Operation: {}", operation);
        log.debug("Entity: {}", entity);
        log.debug("Execution Time: {} ms", executionTime);
        if (result != null) {
            log.debug("Result: {}", gson.toJson(result));
        }
        log.debug("=== DATABASE OPERATION END ===");
    }

    /**
     * Log error with full stack trace
     */
    public static void logError(Logger log, String message, Exception exception) {
        log.error("=== ERROR OCCURRED ===");
        log.error("Error Message: {}", message);
        log.error("Exception Type: {}", exception.getClass().getName());
        log.error("Exception Details: {}", exception.getMessage());
        log.error("Stack Trace: ", exception);
        log.error("Timestamp: {}", System.currentTimeMillis());
        log.error("=== ERROR END ===");
    }

    /**
     * Log access control check
     */
    public static void logAccessControl(Logger log, String empId, String permission, boolean granted) {
        log.info("=== ACCESS CONTROL CHECK ===");
        log.info("Employee ID: {}", empId);
        log.info("Permission Required: {}", permission);
        log.info("Access Granted: {}", granted);
        log.info("Timestamp: {}", System.currentTimeMillis());
    }

    /**
     * Log authentication attempt
     */
    public static void logAuthenticationAttempt(Logger log, String loginId, boolean success, String reason) {
        log.info("=== AUTHENTICATION ATTEMPT ===");
        log.info("Login ID: {}", loginId);
        log.info("Success: {}", success);
        if (!success) {
            log.warn("Failure Reason: {}", reason);
        }
        log.info("Timestamp: {}", System.currentTimeMillis());
    }

    /**
     * Log performance metrics
     */
    public static void logPerformance(Logger log, String operation, long startTime, long endTime) {
        long duration = endTime - startTime;
        log.info("=== PERFORMANCE METRICS ===");
        log.info("Operation: {}", operation);
        log.info("Duration: {} ms", duration);
        if (duration > 1000) {
            log.warn("SLOW OPERATION DETECTED: {} took {} ms", operation, duration);
        }
        log.info("=== PERFORMANCE END ===");
    }

    /**
     * Log data validation
     */
    public static void logValidation(Logger log, String entity, String field, boolean valid, String message) {
        log.debug("=== VALIDATION CHECK ===");
        log.debug("Entity: {}", entity);
        log.debug("Field: {}", field);
        log.debug("Valid: {}", valid);
        if (!valid) {
            log.warn("Validation Error: {}", message);
        }
        log.debug("=== VALIDATION END ===");
    }

    /**
     * Log business logic execution
     */
    public static void logBusinessLogic(Logger log, String operation, String details) {
        log.info("=== BUSINESS LOGIC ===");
        log.info("Operation: {}", operation);
        log.info("Details: {}", details);
        log.info("Timestamp: {}", System.currentTimeMillis());
    }
}
