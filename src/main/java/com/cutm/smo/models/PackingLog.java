package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Packing station detail log.
 * Tracks carton-level packing metrics alongside the daily_stock entry for PKG.
 *
 * Packing operations tracked:
 *   Collar Stay & Tissue Insert → Price Tag Attach → Folding on Board
 *   → Polybag Insertion → Box Packing → Carton Sealing
 */
@Data
@Entity
@Table(name = "packing_log",
        indexes = @Index(name = "idx_pack_date", columnList = "pack_date"))
public class PackingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pack_id")
    private Long packId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_stock_id")
    private DailyStock dailyStock;

    @Column(name = "pack_date", nullable = false)
    private LocalDate packDate;

    @Column(name = "cartons_packed", nullable = false)
    private Integer cartonsPacked = 0;

    @Column(name = "cartons_sealed", nullable = false)
    private Integer cartonSealed = 0;

    @Column(name = "pieces_per_carton", nullable = false)
    private Integer piecesPerCarton;

    @Column(name = "logged_by")
    private Long loggedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
