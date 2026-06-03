package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_stock_ledger")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyStockLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_id")
    private Long ledgerId;

    @Column(name = "ledger_date", nullable = false)
    private LocalDate ledgerDate;

    @Column(name = "location_type", nullable = false, length = 50)
    private String locationType;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType;

    @Column(name = "opening_stock", nullable = false)
    private Integer openingStock = 0;

    @Column(name = "received_qty", nullable = false)
    private Integer receivedQty = 0;

    @Column(name = "issued_qty", nullable = false)
    private Integer issuedQty = 0;

    @Column(name = "adjusted_qty")
    private Integer adjustedQty = 0;

    @Column(name = "closing_stock", nullable = false)
    private Integer closingStock = 0;

    @Column(name = "unit", length = 20)
    private String unit = "PIECES";

    @Column(name = "operation_id")
    private Long operationId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "routing_id")
    private Long routingId;

    @Column(name = "stock_status", length = 20)
    private String stockStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
