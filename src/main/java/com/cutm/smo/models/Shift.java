package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * A single shift record. Shift duration = 480 min (8 hours).
 * shift_type: MORNING | EVENING
 */
@Data
@Entity
@Table(name = "shift",
        uniqueConstraints = @UniqueConstraint(name = "uk_shift_date_type",
                columnNames = {"shift_date", "shift_type"}))
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    // MORNING | EVENING
    @Column(name = "shift_type", nullable = false, length = 20)
    private String shiftType;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
