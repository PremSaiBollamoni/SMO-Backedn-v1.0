package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "operation_stock_limits")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationStockLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "limit_id")
    private Long limitId;

    @Column(name = "operation_id", nullable = false, unique = true)
    private Long operationId;

    @Column(name = "min_qty_per_day")
    private Integer minQtyPerDay = 0;

    @Column(name = "max_qty_per_day")
    private Integer maxQtyPerDay = 0;

    @Column(name = "min_qty_per_month")
    private Integer minQtyPerMonth = 0;

    @Column(name = "max_qty_per_month")
    private Integer maxQtyPerMonth = 0;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 0;

    @Column(name = "high_stock_threshold")
    private Integer highStockThreshold = 0;

    @Column(name = "unit", length = 20)
    private String unit = "PIECES";

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
