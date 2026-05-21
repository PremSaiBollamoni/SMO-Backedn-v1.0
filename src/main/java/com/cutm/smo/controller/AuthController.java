package com.cutm.smo.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cutm.smo.dto.LoginRequest;
import com.cutm.smo.dto.LoginResponse;
import com.cutm.smo.services.AuthService;
import com.cutm.smo.util.LoggingUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            LoggingUtil.logApiRequest(log, "/api/auth/login", "POST", request, request.getLoginid());
            
            LoginResponse response = authService.login(request);
            
            long endTime = System.currentTimeMillis();
            LoggingUtil.logApiResponse(log, "/api/auth/login", 200, response);
            LoggingUtil.logPerformance(log, "Login Operation", startTime, endTime);
            
            return response;
        } catch (Exception e) {
            LoggingUtil.logError(log, "Login failed for user: " + request.getLoginid(), e);
            throw e;
        }
    }
}
