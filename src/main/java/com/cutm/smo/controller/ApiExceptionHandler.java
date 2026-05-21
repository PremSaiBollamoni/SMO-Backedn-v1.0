package com.cutm.smo.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status).body(body(status, ex.getReason(), request.getRequestURI()));
    }

    @ExceptionHandler({
            IdentifierGenerationException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, "Invalid request payload or parameters", request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(HttpStatus.CONFLICT, "Data integrity violation", request.getRequestURI()));
    }

    @ExceptionHandler(JpaSystemException.class)
    public ResponseEntity<Map<String, Object>> handleJpaSystem(
            JpaSystemException ex,
            HttpServletRequest request) {
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
        // Log the actual exception for debugging
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error: " + ex.getMessage(), request.getRequestURI()));
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
