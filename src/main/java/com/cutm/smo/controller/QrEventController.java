package com.cutm.smo.controller;

import com.cutm.smo.models.QrEvent;
import com.cutm.smo.repositories.QrEventRepository;
import com.cutm.smo.util.LoggingUtil;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/qr_event")
@CrossOrigin(origins = "*")
public class QrEventController {
    private final QrEventRepository qrEventRepository;

    public QrEventController(QrEventRepository qrEventRepository) { this.qrEventRepository = qrEventRepository; }

    @GetMapping
    public List<QrEvent> getAllQrEvents() {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET ALL QR EVENTS START ===");
            List<QrEvent> events = qrEventRepository.findAll();
            log.info("Retrieved {} QR events", events.size());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get All QR Events", startTime, endTime);
            log.info("=== GET ALL QR EVENTS END - SUCCESS ===");
            return events;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get all QR events", e);
            LoggingUtil.logPerformance(log, "Get All QR Events (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public QrEvent getQrEventById(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET QR EVENT BY ID START ===");
            log.debug("QR Event ID: {}", id);
            QrEvent event = qrEventRepository.findById(id).orElse(null);
            if (event != null) {
                log.info("QR event found with ID: {}", id);
            } else {
                log.warn("QR event not found with ID: {}", id);
            }
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get QR Event By ID", startTime, endTime);
            log.info("=== GET QR EVENT BY ID END - SUCCESS ===");
            return event;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get QR event with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Get QR Event By ID (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PostMapping
    public QrEvent createQrEvent(@RequestBody QrEvent qrEvent) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE QR EVENT START ===");
            log.debug("QR Event Data: {}", qrEvent);
            QrEvent createdEvent = qrEventRepository.save(qrEvent);
            log.info("QR event created successfully with ID: {}", createdEvent.getEventId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create QR Event", startTime, endTime);
            log.info("=== CREATE QR EVENT END - SUCCESS ===");
            return createdEvent;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to create QR event", e);
            LoggingUtil.logPerformance(log, "Create QR Event (Failed)", startTime, endTime);
            throw e;
        }
    }
}