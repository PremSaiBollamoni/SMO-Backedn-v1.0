package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * QC Verification shift log.
 * Alert rule: rework_pct > 5% triggers supervisor alert.
 * rework_pct = (rework_count / total_inspected) × 100
 */
@Data
@Entity
@Table(name = "qc_log",
        uniqueConstraints = @UniqueConstraint(name = "uk_qc_shift",
                columnNames = "shift_id"))
public class QcLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qc_id")
    private Long qcId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Column(name = "total_inspected", nullable = false)
    private Integer totalInspected = 0;

    @Column(name = "pass_count", nullable = false)
    private Integer passCount = 0;

    @Column(name = "defect_count", nullable = false)
    private Integer defectCount = 0;

    @Column(name = "rework_count", nullable = false)
    private Integer reworkCount = 0;

    // Computed: (rework_count / total_inspected) × 100
    @Column(name = "rework_pct", precision = 6, scale = 2)
    private BigDecimal reworkPct;

    // true when rework_pct > 5
    @Column(name = "alert_triggered", nullable = false)
    private Boolean alertTriggered = false;

    @Column(name = "marked_by", nullable = false)
    private Long markedBy;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
