package com.cutm.smo.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Rate Limiting Filter using Bucket4j for protecting API endpoints.
 * Implements separate rate limits for login attempts and general API calls.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cacheBuckets = new ConcurrentHashMap<>();

    // Rate limiting configuration
    private static final int LOGIN_RATE_LIMIT = 5; // 5 attempts per minute
    private static final int LOGIN_RATE_DURATION_MINUTES = 1;
    private static final int API_RATE_LIMIT = 100; // 100 requests per minute
    private static final int API_RATE_DURATION_MINUTES = 1;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        try {
            if (path.startsWith("/api/auth/login")) {
                // Apply stricter rate limiting to login endpoint
                if (!allowLoginRequest(clientIp)) {
                    log.warn("Login rate limit exceeded for IP: {}", clientIp);
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Too many login attempts. Please try again later.\"}");
                    return;
                }
            } else if (path.startsWith("/api/")) {
                // Apply general rate limiting to API endpoints
                Object empId = request.getAttribute("empId");
                String rateLimitKey = empId != null ? String.valueOf(empId) : clientIp;

                if (!allowApiRequest(rateLimitKey)) {
                    log.warn("API rate limit exceeded for user/IP: {}", rateLimitKey);
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"API rate limit exceeded. Please try again later.\"}");
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Error in rate limiting filter", e);
            // Allow request to proceed if rate limiting fails
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if login request is allowed based on rate limit.
     *
     * @param clientIp the client IP address
     * @return true if request is allowed, false if rate limit exceeded
     */
    private boolean allowLoginRequest(String clientIp) {
        String key = "login:" + clientIp;
        Bucket bucket = cacheBuckets.computeIfAbsent(key, k -> createLoginBucket());
        return bucket.tryConsume(1);
    }

    /**
     * Check if API request is allowed based on rate limit.
     *
     * @param identifier the user ID or IP address
     * @return true if request is allowed, false if rate limit exceeded
     */
    private boolean allowApiRequest(String identifier) {
        String key = "api:" + identifier;
        Bucket bucket = cacheBuckets.computeIfAbsent(key, k -> createApiBucket());
        return bucket.tryConsume(1);
    }

    /**
     * Create a bucket for login rate limiting.
     *
     * @return Bucket configured for login rate limiting
     */
    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.classic(LOGIN_RATE_LIMIT, Refill.intervally(LOGIN_RATE_LIMIT, Duration.ofMinutes(LOGIN_RATE_DURATION_MINUTES)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create a bucket for API rate limiting.
     *
     * @return Bucket configured for API rate limiting
     */
    private Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.classic(API_RATE_LIMIT, Refill.intervally(API_RATE_LIMIT, Duration.ofMinutes(API_RATE_DURATION_MINUTES)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Extract client IP address from request, handling proxy headers.
     *
     * @param request the HTTP request
     * @return client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Don't filter health and swagger endpoints
        return path.startsWith("/api/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/v3/api-docs");
    }
}
