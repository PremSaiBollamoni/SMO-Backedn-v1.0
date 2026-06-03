package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stockmovement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movement_id")
    private Long movementId;

    @Column(name = "movement_type", nullable = false, length = 50)
    private String movementType; // RECEIPT, ISSUE, ADJUSTMENT, TRANSFER

    @Column(name = "transaction_type", length = 50)
    private String transactionType; // PURCHASE, PRODUCTION_USE, STOCK_CORRECTION, etc.

    @Column(name = "from_location_type", length = 50)
    private String fromLocationType;

    @Column(name = "from_location_id")
    private Long fromLocationId;

    @Column(name = "to_location_type", length = 50)
    private String toLocationType;

    @Column(name = "to_location_id")
    private Long toLocationId;

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType; // RAW_MATERIAL, WIP, FINISHED_GOODS

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "style_variant_id")
    private Long styleVariantId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "routing_id")
    private Long routingId;

    @Column(name = "operation_id")
    private Long operationId;

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
