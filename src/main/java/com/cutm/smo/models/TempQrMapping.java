package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Maps a physical temp QR card (EMP-TEMP-001) to an employee for a given day.
 * Freed at end of day so the card can be reassigned next morning.
 */
@Data
@Entity
@Table(name = "temp_qr_mapping",
        uniqueConstraints = @UniqueConstraint(name = "uk_qr_date",
                columnNames = {"qr_token", "mapping_date"}))
public class TempQrMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Long mappingId;

    @Column(name = "qr_token", nullable = false, length = 50)
    private String qrToken;

    @Column(name = "emp_id", nullable = false)
    private Long empId;

    @Column(name = "mapping_date", nullable = false)
    private LocalDate mappingDate;

    // false = active for today, true = freed at end of day
    @Column(name = "is_freed", nullable = false)
    private boolean freed = false;

    @Column(name = "mapped_by")
    private Long mappedBy;

    @Column(name = "mapped_at", nullable = false, updatable = false)
    private LocalDateTime mappedAt;

    @PrePersist
    void prePersist() {
        mappedAt = LocalDateTime.now();
    }
}
