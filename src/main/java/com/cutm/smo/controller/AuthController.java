package com.cutm.smo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

import com.cutm.smo.dto.LoginRequest;
import com.cutm.smo.dto.LoginResponse;
import com.cutm.smo.services.AuthService;
import com.cutm.smo.util.LoggingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Employee login and authentication endpoints")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @io.swagger.v3.oas.annotations.Operation(summary = "Employee Login", description = "Authenticate employee with credentials and return access token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "400", description = "Missing required fields")
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            if (request.getLoginid() == null || request.getLoginid().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "loginid is required");
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is required");
            }

            LoggingUtil.logApiRequest(log, "/api/auth/login", "POST", request, request.getLoginid());

            LoginResponse response = authService.login(request);

            long endTime = System.currentTimeMillis();
            LoggingUtil.logApiResponse(log, "/api/auth/login", 200, response);
            LoggingUtil.logPerformance(log, "Login Operation", startTime, endTime);

            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            LoggingUtil.logError(log, "Login failed", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred during login");
        }
    }
}
