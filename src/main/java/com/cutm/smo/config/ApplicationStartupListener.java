package com.cutm.smo.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener to log application startup completion
 */
@Slf4j
@Component
public class ApplicationStartupListener {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${app.base-url:http://localhost}")
    private String baseUrl;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String fullUrl = baseUrl + ":" + serverPort;
        
        log.info("SMO Backend started successfully at {}", fullUrl);
        log.info("Health endpoint: {}/api/health", fullUrl);
        log.info("API base: {}/api", fullUrl);
    }
}

