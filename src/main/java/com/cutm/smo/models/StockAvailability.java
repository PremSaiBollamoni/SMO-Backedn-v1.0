package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Pre-shift stock availability check for each post-sewing station.
 * status: AVAILABLE | NO_STOCK
 * If NO_STOCK: station is marked Starved → supervisor alert logged.
 *
 * station_code: FGS | WSH | IRN_R | QCV | IRN_F | PKG | RTM
 */
@Data
@Entity
@Table(name = "stock_availability",
        uniqueConstraints = @UniqueConstraint(name = "uk_stock_avail_shift_station",
                columnNames = {"shift_id", "station_code"}))
public class StockAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "avail_id")
    private Long availId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    // FGS | WSH | IRN_R | QCV | IRN_F | PKG | RTM
    @Column(name = "station_code", nullable = false, length = 20)
    private String stationCode;

    // AVAILABLE | NO_STOCK
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "marked_by", nullable = false)
    private Long markedBy;

    @Column(name = "marked_at", nullable = false, updatable = false)
    private LocalDateTime markedAt;

    @PrePersist
    void prePersist() {
        markedAt = LocalDateTime.now();
    }
}
