package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "poitems")
public class PoItems {
    @Id
    @Column(name = "po_item_id")
    private Long poItemId;

    @Column(name = "po_id")
    private Long poId;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "style_variant_id")
    private Long styleVariantId;

    @Column(name = "qty")
    private Integer qty;
}
