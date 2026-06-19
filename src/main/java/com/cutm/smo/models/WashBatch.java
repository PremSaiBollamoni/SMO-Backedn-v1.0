package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Washing station batch record.
 * Tracks wash cycles, batch size, cycle time, and pieces washed.
 * Linked to daily_stock for the WSH station on a given date.
 */
@Data
@Entity
@Table(name = "wash_batch",
        indexes = @Index(name = "idx_wash_date", columnList = "wash_date"))
public class WashBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Long batchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_stock_id")
    private DailyStock dailyStock;

    @Column(name = "batch_no", nullable = false, length = 30)
    private String batchNo;

    // Duration of one wash cycle in minutes
    @Column(name = "cycle_time_min", nullable = false)
    private Integer cycleTimeMin;

    @Column(name = "batch_size", nullable = false)
    private Integer batchSize;

    @Column(name = "pieces_washed", nullable = false)
    private Integer piecesWashed;

    @Column(name = "wash_date", nullable = false)
    private LocalDate washDate;

    @Column(name = "logged_by")
    private Long loggedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
