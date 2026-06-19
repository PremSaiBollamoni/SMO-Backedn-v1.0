package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Method study work order — triggered automatically when efficiency < 50%.
 *
 * Lifecycle: OPEN → ASSIGNED → IN_PROGRESS → CLOSED
 *
 * trigger_date: the shift date when efficiency dropped below threshold.
 * assigned_to:  IE/SAM Analyst emp_id who will conduct the study.
 */
@Data
@Entity
@Table(name = "method_study",
        indexes = {
            @Index(name = "idx_ms_ws",     columnList = "ws_id"),
            @Index(name = "idx_ms_status", columnList = "status")
        })
public class MethodStudy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ms_id")
    private Long msId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ws_id", nullable = false)
    private Workstation workstation;

    @Column(name = "emp_id", nullable = false)
    private Long empId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    // Efficiency that triggered this work order
    @Column(name = "efficiency_pct", nullable = false, precision = 7, scale = 2)
    private BigDecimal efficiencyPct;

    @Column(name = "trigger_date", nullable = false)
    private LocalDate triggerDate;

    // IE analyst assigned
    @Column(name = "assigned_to")
    private Long assignedTo;

    // OPEN | ASSIGNED | IN_PROGRESS | CLOSED
    @Column(name = "status", nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
