package com.cutm.smo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token Provider for generating, validating, and refreshing JWT tokens.
 * Provides secure token management for authentication and authorization.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final String jwtSecret;
    private final long jwtExpirationMs;
    private final long refreshTokenExpirationMs;
    private final SecretKey secretKey;

    public JwtTokenProvider(
            @Value("${jwt.secret:your-secret-key-change-this-in-production-environment-with-at-least-256-bits}") String jwtSecret,
            @Value("${jwt.expiration:86400000}") long jwtExpirationMs,
            @Value("${jwt.refresh-expiration:604800000}") long refreshTokenExpirationMs) {
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        // Create a SecretKey from the provided secret with at least 256 bits
        this.secretKey = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Generate a JWT access token for the given employee with role and activities claims.
     *
     * @param empId the employee ID
     * @param empName the employee name
     * @param role the employee role
     * @param activities the employee activities/permissions
     * @return JWT token string
     */
    public String generateToken(Long empId, String empName, String role, String activities) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("empId", empId);
        claims.put("empName", empName);
        claims.put("role", role);
        claims.put("activities", activities != null ? activities : "");

        return createToken(claims, String.valueOf(empId));
    }

    /**
     * Generate a refresh token for the given employee ID.
     *
     * @param empId the employee ID
     * @return refresh token string
     */
    public String generateRefreshToken(Long empId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return createRefreshToken(claims, String.valueOf(empId));
    }

    /**
     * Create an access token with the given claims and subject.
     *
     * @param claims the token claims
     * @param subject the token subject (employee ID)
     * @return JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(jwtExpirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Create a refresh token with the given claims and subject.
     *
     * @param claims the token claims
     * @param subject the token subject (employee ID)
     * @return refresh token string
     */
    private String createRefreshToken(Map<String, Object> claims, String subject) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(refreshTokenExpirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extract the employee ID from the JWT token.
     *
     * @param token the JWT token
     * @return employee ID
     */
    public Long getEmpIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object empId = claims.get("empId");
        if (empId instanceof Number) {
            return ((Number) empId).longValue();
        }
        return Long.parseLong(empId.toString());
    }

    /**
     * Extract the employee name from the JWT token.
     *
     * @param token the JWT token
     * @return employee name
     */
    public String getEmpNameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("empName");
    }

    /**
     * Extract the role from the JWT token.
     *
     * @param token the JWT token
     * @return role name
     */
    public String getRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("role");
    }

    /**
     * Extract the activities from the JWT token.
     *
     * @param token the JWT token
     * @return activities/permissions
     */
    public String getActivitiesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object activities = claims.get("activities");
        return activities != null ? (String) activities : "";
    }

    /**
     * Get the expiration date of the token.
     *
     * @param token the JWT token
     * @return token expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * Validate the JWT token - checks signature and expiration.
     *
     * @param token the JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if the token is expired.
     *
     * @param token the JWT token
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            log.warn("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Get all claims from the JWT token.
     *
     * @param token the JWT token
     * @return claims
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get the token expiration time in milliseconds.
     *
     * @return expiration time
     */
    public long getTokenExpirationMs() {
        return jwtExpirationMs;
    }
}
