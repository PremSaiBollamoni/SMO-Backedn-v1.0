package com.cutm.smo.services;

import com.cutm.smo.models.QrEvent;
import com.cutm.smo.repositories.QrEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class QrEventService {
    
    private final QrEventRepository qrEventRepository;
    
    public QrEventService(QrEventRepository qrEventRepository) {
        this.qrEventRepository = qrEventRepository;
    }
    
    /**
     * Log a QR event to the audit trail
     * @param qrCode The QR code that was scanned
     * @param entityType Type of entity (BIN, WIP, MERGE, etc.)
     * @param entityId ID of the entity
     * @param eventType Type of event (ASSIGNMENT, TRACKING, MERGE_SOURCE, MERGE_TARGET, etc.)
     * @param operationId Operation ID (optional)
     * @param machineId Machine ID (optional)
     * @param operatorId Operator/Employee ID (optional)
     * @param styleId Style ID (optional)
     */
    @Transactional
    public void logQrEvent(
            String qrCode,
            String entityType,
            Long entityId,
            String eventType,
            Long operationId,
            Long machineId,
            Long operatorId,
            Long styleId
    ) {
        try {
            QrEvent event = new QrEvent();
            event.setEventId(System.currentTimeMillis()); // Generate unique event ID
            event.setQrCode(qrCode);
            event.setEntityType(entityType);
            event.setEntityId(entityId);
            event.setEventType(eventType);
            event.setOperationId(operationId);
            event.setMachineId(machineId);
            event.setOperatorId(operatorId);
            event.setStyleId(styleId);
            event.setTimestamp(LocalDateTime.now());
            
            qrEventRepository.save(event);
            
            log.info("QR Event logged: eventType={}, qrCode={}, entityType={}, entityId={}", 
                    eventType, qrCode, entityType, entityId);
        } catch (Exception e) {
            // Log error but don't fail the main operation
            log.error("Failed to log QR event: eventType={}, qrCode={}, error={}", 
                    eventType, qrCode, e.getMessage());
        }
    }
    
    /**
     * Simplified method for logging basic QR events without optional parameters
     */
    @Transactional
    public void logQrEvent(String qrCode, String entityType, Long entityId, String eventType) {
        logQrEvent(qrCode, entityType, entityId, eventType, null, null, null, null);
    }
}
