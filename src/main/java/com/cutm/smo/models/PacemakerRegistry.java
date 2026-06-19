package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pacemaker registry — operators who achieve efficiency ≥ 150%.
 * They are Trainer Candidates and Recognition-Eligible per PALMS policy.
 * One entry per operator per shift (a new record each qualifying shift).
 */
@Data
@Entity
@Table(name = "pacemaker_registry",
        indexes = {
            @Index(name = "idx_pm_emp",  columnList = "emp_id"),
            @Index(name = "idx_pm_date", columnList = "recorded_date")
        })
public class PacemakerRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pm_id")
    private Long pmId;

    @Column(name = "emp_id", nullable = false)
    private Long empId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ws_id", nullable = false)
    private Workstation workstation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Column(name = "efficiency_pct", nullable = false, precision = 7, scale = 2)
    private BigDecimal efficiencyPct;

    @Column(name = "recorded_date", nullable = false)
    private LocalDate recordedDate;

    @Column(name = "is_trainer_candidate", nullable = false)
    private Boolean isTrainerCandidate = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
