package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Shift-level efficiency per operator/workstation.
 *
 * Formula: efficiency_pct = (total_sam_earned / time_on_machine_min) × 100
 *
 * classification:
 *   CRITICAL   < 50%   → method study triggered
 *   LOW        50–84%  → coaching
 *   MARGINAL   85–99%  → monitor
 *   GOOD       100–149% → maintain
 *   EXCELLENT  ≥150%   → pacemaker candidate
 */
@Data
@Entity
@Table(name = "efficiency_summary",
        uniqueConstraints = @UniqueConstraint(name = "uk_eff_shift_ws",
                columnNames = {"shift_id", "ws_id"}),
        indexes = @Index(name = "idx_eff_emp", columnList = "emp_id"))
public class EfficiencySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "eff_id")
    private Long effId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ws_id", nullable = false)
    private Workstation workstation;

    @Column(name = "emp_id", nullable = false)
    private Long empId;

    @Column(name = "total_pieces", nullable = false)
    private Integer totalPieces = 0;

    @Column(name = "total_sam_earned", precision = 10, scale = 3)
    private BigDecimal totalSamEarned = BigDecimal.ZERO;

    // Actual minutes operator was on machine (≤480 for 8-hour shift)
    @Column(name = "time_on_machine_min", precision = 8, scale = 2)
    private BigDecimal timeOnMachineMin;

    @Column(name = "efficiency_pct", precision = 7, scale = 2)
    private BigDecimal efficiencyPct;

    // CRITICAL | LOW | MARGINAL | GOOD | EXCELLENT
    @Column(name = "classification", length = 20)
    private String classification;

    // Whether this summary is a pacemaker (efficiency ≥ 150%)
    @Column(name = "is_pacemaker", nullable = false)
    private Boolean isPacemaker = false;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    @PrePersist
    @PreUpdate
    void stamp() {
        computedAt = LocalDateTime.now();
    }
}
