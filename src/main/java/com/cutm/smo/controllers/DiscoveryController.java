package com.cutm.smo.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Discovery Controller for SMO Backend
 * Provides endpoints for service discovery and validation
 */
@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DiscoveryController {

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${smo.service.name:SMO-Backend}")
    private String serviceName;

    @Value("${smo.service.version:1.0.0}")
    private String serviceVersion;

    /**
     * Service info endpoint for discovery validation
     */
    @GetMapping("/discovery/info")
    public ResponseEntity<Map<String, Object>> serviceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", serviceName);
        info.put("serviceType", "_smo._tcp.local.");
        info.put("version", serviceVersion);
        info.put("port", serverPort);
        info.put("endpoints", Map.of(
            "health", "/api/health",
            "auth", "/api/auth/login",
            "processplan", "/api/processplan",
            "production", "/api/production",
            "supervisor", "/api/supervisor",
            "hr", "/api/hr"
        ));
        
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            info.put("host", localHost.getHostAddress());
            info.put("baseUrl", "http://" + localHost.getHostAddress() + ":" + serverPort);
        } catch (UnknownHostException e) {
            info.put("host", "unknown");
        }
        
        return ResponseEntity.ok(info);
    }

    /**
     * Discovery ping endpoint for network scanning
     */
    @GetMapping("/discovery/ping")
    public ResponseEntity<Map<String, String>> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("service", "SMO-Backend");
        response.put("type", "_smo._tcp.local.");
        response.put("status", "available");
        response.put("protocol", "http");
        return ResponseEntity.ok(response);
    }
}