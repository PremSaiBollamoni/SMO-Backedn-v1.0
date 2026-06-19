package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ready To Market stock — cumulative, does NOT reset daily.
 * Grouped by style / size / colour / order / buyer.
 * closing_stock = opening_stock + production_added - dispatched
 *
 * RTM report generated daily at 17:00.
 */
@Data
@Entity
@Table(name = "rtm_stock",
        indexes = {
            @Index(name = "idx_rtm_order",  columnList = "order_no"),
            @Index(name = "idx_rtm_buyer",  columnList = "buyer_name"),
            @Index(name = "idx_rtm_date",   columnList = "stock_date")
        })
public class RtmStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rtm_id")
    private Long rtmId;

    @Column(name = "style", nullable = false, length = 50)
    private String style;

    @Column(name = "size", nullable = false, length = 20)
    private String size;

    @Column(name = "colour", length = 50)
    private String colour;

    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;

    @Column(name = "buyer_name", nullable = false, length = 100)
    private String buyerName;

    @Column(name = "stock_date", nullable = false)
    private LocalDate stockDate;

    @Column(name = "opening_stock", nullable = false)
    private Integer openingStock = 0;

    @Column(name = "production_added", nullable = false)
    private Integer productionAdded = 0;

    @Column(name = "dispatched", nullable = false)
    private Integer dispatched = 0;

    @Column(name = "closing_stock", nullable = false)
    private Integer closingStock = 0;

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
        closingStock = openingStock + productionAdded - dispatched;
    }
}
