package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "bom")
public class Bom {
    @Id
    @Column(name = "bom_id")
    private Long bomId;

    @Column(name = "style_id")
    private Long styleId;

    @Column(name = "style_variant_id")
    private Long styleVariantId;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "qty")
    private BigDecimal qty;
}
