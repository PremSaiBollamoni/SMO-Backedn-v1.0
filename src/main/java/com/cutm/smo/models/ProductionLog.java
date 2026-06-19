package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Hourly production count per workstation per shift.
 * hour_slot: 1–8 (each representing one hour of the 8-hour shift).
 * sam_earned = pieces_produced × operation.sam
 */
@Data
@Entity
@Table(name = "production_log",
        uniqueConstraints = @UniqueConstraint(name = "uk_prod_shift_ws_hour",
                columnNames = {"shift_id", "ws_id", "hour_slot"}),
        indexes = @Index(name = "idx_prod_shift_ws", columnList = "shift_id,ws_id"))
public class ProductionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ws_id", nullable = false)
    private Workstation workstation;

    @Column(name = "emp_id", nullable = false)
    private Long empId;

    // 1–8 (hour number within the shift)
    @Column(name = "hour_slot", nullable = false)
    private Integer hourSlot;

    @Column(name = "pieces_produced", nullable = false)
    private Integer piecesProduced = 0;

    // pieces_produced × SAM of the operation
    @Column(name = "sam_earned", precision = 10, scale = 3)
    private BigDecimal samEarned = BigDecimal.ZERO;

    @Column(name = "logged_at", nullable = false, updatable = false)
    private LocalDateTime loggedAt;

    @PrePersist
    void prePersist() {
        loggedAt = LocalDateTime.now();
    }
}
