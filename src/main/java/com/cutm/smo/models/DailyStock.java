package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Daily stock position for each post-sewing station.
 * Formula: closing_balance = opening_balance + additions - dispatched
 *
 * station_code: FGS | WSH | IRN_R | QCV | IRN_F | PKG | RTM
 *
 * Alert thresholds (from PALMS spec):
 *   FGS    > 50 pcs
 *   WSH    > 200 pcs
 *   IRN_R  > 180 pcs
 *   QCV    rework > 5%  (handled in QcLog)
 *   IRN_F  > 160 pcs
 *   PKG    > 240 pcs
 *   RTM    cumulative, does NOT reset daily
 */
@Data
@Entity
@Table(name = "daily_stock",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_stock_station_date",
                columnNames = {"station_code", "stock_date"}))
public class DailyStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long stockId;

    @Column(name = "station_code", nullable = false, length = 20)
    private String stationCode;

    @Column(name = "stock_date", nullable = false)
    private LocalDate stockDate;

    @Column(name = "opening_balance", nullable = false)
    private Integer openingBalance = 0;

    @Column(name = "additions", nullable = false)
    private Integer additions = 0;

    @Column(name = "dispatched", nullable = false)
    private Integer dispatched = 0;

    @Column(name = "closing_balance", nullable = false)
    private Integer closingBalance = 0;

    // Highest WIP reached during the day
    @Column(name = "max_wip_reached")
    private Integer maxWipReached;

    // Whether closing_balance exceeded the PALMS alert threshold
    @Column(name = "alert_triggered", nullable = false)
    private Boolean alertTriggered = false;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "updated_by")
    private Long updatedBy;

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
        closingBalance = openingBalance + additions - dispatched;
    }
}
