package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Time study observation record for an operation.
 * Minimum 30 observations required before status = APPROVED.
 * Derived values (sam_source = DERIVED) must be replaced within 5 working days.
 * status: PENDING_STUDY | APPROVED | REJECTED
 */
@Data
@Entity
@Table(name = "sam_study",
        indexes = @Index(name = "idx_sam_study_op", columnList = "op_id"))
public class SamStudy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_id")
    private Long studyId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "op_id", nullable = false)
    private Operation operation;

    // Average observed time in minutes
    @Column(name = "observed_time_min", nullable = false, precision = 8, scale = 3)
    private BigDecimal observedTimeMin;

    @Column(name = "observation_count", nullable = false)
    private Integer observationCount;

    // Computed SAM = observed_time × (1 + pf_d_pct/100)
    @Column(name = "computed_sam", precision = 8, scale = 3)
    private BigDecimal computedSam;

    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    @Column(name = "studied_by", nullable = false)
    private Long studiedBy;

    // PENDING_STUDY | APPROVED | REJECTED
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING_STUDY";

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
