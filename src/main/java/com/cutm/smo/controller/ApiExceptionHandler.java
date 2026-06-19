package com.cutm.smo.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        log.warn("ResponseStatusException: status={}, reason={}, path={}", status, ex.getReason(), request.getRequestURI());
        return ResponseEntity.status(status).body(body(status, ex.getReason(), request.getRequestURI()));
    }

    @ExceptionHandler({
            IdentifierGenerationException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("BadRequest exception: type={}, path={}", ex.getClass().getSimpleName(), request.getRequestURI());
        log.debug("BadRequest details: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, "Invalid request payload or parameters", request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        log.warn("DataIntegrityViolationException: path={}", request.getRequestURI());
        log.debug("DataIntegrity details: {}", ex.getMessage());
        String msg = "Data integrity violation";
        String cause = ex.getMostSpecificCause().getMessage();
        if (cause != null) {
            String c = cause.toLowerCase();
            if (c.contains("uk_employee_phone") || c.contains("phone")) {
                msg = "Phone number already exists. Use a different phone number.";
            } else if (c.contains("email") || c.contains("uk_employee_email")) {
                msg = "Email address already exists. Use a different email.";
            } else if (c.contains("emp_id") || c.contains("primary") || c.contains("duplicate entry")) {
                msg = "Employee ID already exists. Use a different Employee ID.";
            } else if (c.contains("aadhar")) {
                msg = "Aadhar number already exists.";
            } else if (c.contains("pan")) {
                msg = "PAN card number already exists.";
            } else if (c.contains("unique") || c.contains("duplicate")) {
                msg = "A record with this data already exists. Check for duplicate values.";
            }
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(HttpStatus.CONFLICT, msg, request.getRequestURI()));
    }

    @ExceptionHandler(JpaSystemException.class)
    public ResponseEntity<Map<String, Object>> handleJpaSystem(
            JpaSystemException ex,
            HttpServletRequest request) {
        log.warn("JpaSystemException: path={}", request.getRequestURI());
        log.debug("JpaSystemException details: {}", ex.getMessage());
        Throwable root = ex.getMostSpecificCause();
        if (root instanceof IdentifierGenerationException
                || ex.getMessage().contains("must be manually assigned")
                || ex.getMessage().contains("not-null property references a null")
                || ex.getMessage().contains("identifier")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(body(HttpStatus.BAD_REQUEST, "Missing or invalid required identifiers", request.getRequestURI()));
        }
        if (root instanceof ConstraintViolationException || ex.getMessage().contains("constraint")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(body(HttpStatus.CONFLICT, "Data integrity violation", request.getRequestURI()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, "Invalid request for persistence operation", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected exception: path={}, type={}", request.getRequestURI(), ex.getClass().getSimpleName());
        log.error("Exception stack trace:", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred. Please try again later.", request.getRequestURI()));
    }

    private Map<String, Object> body(HttpStatus status, String message, String path) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", LocalDateTime.now());
        map.put("status", status.value());
        map.put("error", status.getReasonPhrase());
        map.put("message", message);
        map.put("path", path);
        return map;
    }
}
