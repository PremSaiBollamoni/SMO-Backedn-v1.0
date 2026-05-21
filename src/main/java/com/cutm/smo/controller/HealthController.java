package com.cutm.smo.controller;

import java.util.Map;
import java.util.HashMap;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cutm.smo.util.LoggingUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${smo.service.name:SMO-Backend}")
    private String serviceName;

    @Value("${smo.service.version:1.0.0}")
    private String serviceVersion;

    @Value("${server.port:8080}")
    private int serverPort;

    @GetMapping("/health")
    public Map<String, Object> health() {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== HEALTH CHECK START ===");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "UP");
            response.put("service", serviceName);
            response.put("version", serviceVersion);
            response.put("timestamp", System.currentTimeMillis());
            
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                response.put("host", localHost.getHostAddress());
                response.put("port", serverPort);
            } catch (UnknownHostException e) {
                response.put("host", "unknown");
            }
            
            log.info("Health check successful");
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Health Check", startTime, endTime);
            log.info("=== HEALTH CHECK END - SUCCESS ===");
            return response;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Health check failed", e);
            LoggingUtil.logPerformance(log, "Health Check (Failed)", startTime, endTime);
            throw e;
        }
    }
}
