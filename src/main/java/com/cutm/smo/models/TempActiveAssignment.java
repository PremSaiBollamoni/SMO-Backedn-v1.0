package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "temp_active_assignments")
public class TempActiveAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "temp_id")
    private Long tempId;

    @Column(name = "machine_qr", nullable = false)
    private String machineQr;

    @Column(name = "tray_qr", nullable = false)
    private String trayQr;

    @Column(name = "emp_id", nullable = false)
    private Long empId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @Column(name = "status")
    private String status = "assigned";

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    protected void onCreate() {
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}