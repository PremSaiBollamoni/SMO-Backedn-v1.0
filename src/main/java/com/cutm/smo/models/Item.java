package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "item")
public class Item {
    @Id
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "name")
    private String name;

    @Column(name = "category")
    private String category;

    @Column(name = "unit")
    private String unit;

    @Column(name = "sourcing_type")
    private String sourcingType;

    @Column(name = "status")
    private String status;
}
