package com.cutm.smo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Enumeration;

/**
 * Interceptor to log all HTTP requests and responses
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);

        logger.info("=== INCOMING HTTP REQUEST ===");
        logger.info("Request ID: {}", request.getRequestId());
        logger.info("Method: {}", request.getMethod());
        logger.info("URI: {}", request.getRequestURI());
        logger.info("Query String: {}", request.getQueryString());
        logger.info("Remote Address: {}", request.getRemoteAddr());
        logger.info("Remote Host: {}", request.getRemoteHost());
        logger.info("Content Type: {}", request.getContentType());

        // Log headers
        logger.debug("=== REQUEST HEADERS ===");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            if (!headerName.toLowerCase().contains("password") && !headerName.toLowerCase().contains("authorization")) {
                logger.debug("{}: {}", headerName, headerValue);
            } else {
                logger.debug("{}: [REDACTED]", headerName);
            }
        }

        // Log parameters
        logger.debug("=== REQUEST PARAMETERS ===");
        request.getParameterMap().forEach((key, values) -> {
            if (!key.toLowerCase().contains("password")) {
                logger.debug("{}: {}", key, String.join(",", values));
            } else {
                logger.debug("{}: [REDACTED]", key);
            }
        });

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        long startTime = (long) request.getAttribute("startTime");
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        logger.info("=== HTTP RESPONSE ===");
        logger.info("Status Code: {}", response.getStatus());
        logger.info("Content Type: {}", response.getContentType());
        logger.info("Duration: {} ms", duration);

        if (duration > 1000) {
            logger.warn("SLOW REQUEST DETECTED: {} {} took {} ms", request.getMethod(), request.getRequestURI(), duration);
        }

        logger.info("=== REQUEST COMPLETE ===");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (ex != null) {
            logger.error("=== REQUEST ERROR ===");
            logger.error("Exception Type: {}", ex.getClass().getName());
            logger.error("Exception Message: {}", ex.getMessage());
            logger.error("Stack Trace: ", ex);
            logger.error("=== REQUEST ERROR END ===");
        }
    }
}
