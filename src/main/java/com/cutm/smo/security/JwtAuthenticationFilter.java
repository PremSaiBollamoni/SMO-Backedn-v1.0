package com.cutm.smo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT Authentication Filter for processing JWT tokens from HTTP requests.
 * Extracts JWT from Authorization header, validates it, and sets up the authentication
 * context for the request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                Long empId = jwtTokenProvider.getEmpIdFromToken(jwt);
                String empName = jwtTokenProvider.getEmpNameFromToken(jwt);
                String role = jwtTokenProvider.getRoleFromToken(jwt);
                String activities = jwtTokenProvider.getActivitiesFromToken(jwt);

                // Create authentication token with employee details
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                empId,
                                null,
                                new ArrayList<>()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Store additional details in session/request attributes
                request.setAttribute("empId", empId);
                request.setAttribute("empName", empName);
                request.setAttribute("role", role);
                request.setAttribute("activities", activities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set authentication for user: {} with role: {}", empId, role);
            } else if (StringUtils.hasText(jwt)) {
                log.warn("JWT validation failed for token");
            }
        } catch (Exception e) {
            log.error("Could not set user authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from the Authorization header.
     * Expected format: "Bearer <token>"
     *
     * @param request the HTTP request
     * @return JWT token or null if not present
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Don't filter login, health, and swagger endpoints
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/v3/api-docs");
    }
}
