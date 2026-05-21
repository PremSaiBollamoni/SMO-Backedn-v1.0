package com.cutm.smo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Service Discovery Configuration for SMO Backend
 * Advertises the backend service on local network using mDNS/Bonjour
 */
@Slf4j
@Configuration
public class ServiceDiscoveryConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${smo.service.name:SMO-Backend}")
    private String serviceName;

    @Value("${smo.service.version:1.0.0}")
    private String serviceVersion;

    @Bean
    public ServiceAdvertiser serviceAdvertiser() {
        return new ServiceAdvertiser();
    }

    @Component
    public class ServiceAdvertiser {
        private JmDNS jmdns;
        private ServiceInfo serviceInfo;

        @EventListener(ApplicationReadyEvent.class)
        public void advertiseService() {
            try {
                // Get local IP address
                InetAddress localHost = InetAddress.getLocalHost();
                
                // Create mDNS instance
                jmdns = JmDNS.create(localHost);
                
                // Service properties for authentication and identification
                Map<String, String> properties = new HashMap<>();
                properties.put("version", serviceVersion);
                properties.put("api", "/api");
                properties.put("health", "/api/health");
                properties.put("auth", "/api/auth/login");
                properties.put("protocol", "http");
                properties.put("app", "SMO-Manufacturing");
                properties.put("secure", "true");
                
                // Create service info
                serviceInfo = ServiceInfo.create(
                    "_smo._tcp.local.",  // Service type
                    serviceName,         // Service name
                    serverPort,          // Port
                    0,                   // Weight
                    0,                   // Priority
                    properties           // Properties
                );
                
                // Register the service
                jmdns.registerService(serviceInfo);
                
                log.info("SMO Service Discovery: {} available at {}:{}", 
                    serviceName, localHost.getHostAddress(), serverPort);
                
            } catch (IOException e) {
                log.error("Failed to start service discovery: {}", e.getMessage());
                log.warn("Service will still work, but auto-discovery won't be available");
            }
        }

        public void shutdown() {
            try {
                if (serviceInfo != null && jmdns != null) {
                    jmdns.unregisterService(serviceInfo);
                    log.info("Service discovery stopped");
                }
                if (jmdns != null) {
                    jmdns.close();
                }
            } catch (IOException e) {
                log.error("Error stopping service discovery: {}", e.getMessage());
            }
        }
    }
}