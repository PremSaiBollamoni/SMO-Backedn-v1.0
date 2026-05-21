package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "inventorystock")
public class InventoryStock {
    @Id
    @Column(name = "stock_id")
    private Long stockId;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "style_variant_id")
    private Long styleVariantId;

    @Column(name = "qty")
    private Integer qty;

    @Column(name = "location")
    private String location;

    @Column(name = "batch")
    private String batch;
}
