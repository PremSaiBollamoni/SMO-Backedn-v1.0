package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One active sewing job: one employee, one station, one tray.
 * status: IN_PROGRESS | COMPLETED
 */
@Data
@Entity
@Table(name = "job_assignment",
        indexes = {
            @Index(name = "idx_job_emp", columnList = "emp_id"),
            @Index(name = "idx_job_status", columnList = "status")
        })
public class JobAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "emp_id", nullable = false)
    private Long empId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ws_id", nullable = false)
    private Workstation workstation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tray_id", nullable = false)
    private Tray tray;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "op_id", nullable = false)
    private Operation operation;

    @Column(name = "bundle_qty", nullable = false)
    private Integer bundleQty;

    // SAM value snapshot at time of assignment (min/pc)
    @Column(name = "sam_value", nullable = false, precision = 8, scale = 3)
    private BigDecimal samValue;

    // Estimated duration = bundleQty × samValue (minutes)
    @Column(name = "est_minutes", nullable = false, precision = 8, scale = 2)
    private BigDecimal estMinutes;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    // Actual minutes taken (set on completion)
    @Column(name = "actual_minutes", precision = 8, scale = 2)
    private BigDecimal actualMinutes;

    // efficiency = (bundleQty × samValue) / actualMinutes × 100
    @Column(name = "efficiency_pct", precision = 7, scale = 2)
    private BigDecimal efficiencyPct;

    // IN_PROGRESS | COMPLETED
    @Column(name = "status", nullable = false, length = 20)
    private String status = "IN_PROGRESS";

    @Column(name = "assigned_by", nullable = false)
    private Long assignedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (startTime == null) startTime = LocalDateTime.now();
    }
}
