package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "grnitems")
public class GrnItems {
    @Id
    @Column(name = "grn_item_id")
    private Long grnItemId;

    @Column(name = "grn_id")
    private Long grnId;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "style_variant_id")
    private Long styleVariantId;

    @Column(name = "qty")
    private Integer qty;
}
