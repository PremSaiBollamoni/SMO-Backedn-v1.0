package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "temp_assignment_log")
public class TempAssignmentLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "machine_qr")
    private String machineQr;

    @Column(name = "tray_qr")
    private String trayQr;

    @Column(name = "emp_id")
    private Long empId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @Column(name = "supervisor_id")
    private Long supervisorId;

    @Column(name = "notes")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (eventTime == null) {
            eventTime = LocalDateTime.now();
        }
    }
}