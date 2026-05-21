package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "qr_event")
public class QrEvent {
    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "qr_code")
    private String qrCode;

    @Column(name = "style_id")
    private Long styleId;

    @Column(name = "operation_id")
    private Long operationId;

    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}
