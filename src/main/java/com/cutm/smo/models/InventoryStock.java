package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventorystock")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long stockId;

    @Column(name = "location_type", nullable = false, length = 50)
    private String locationType;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "qty", nullable = false)
    private Integer qty = 0;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "style_variant_id")
    private Long styleVariantId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "routing_id")
    private Long routingId;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
