package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "bundle")
public class Bundle {
    @Id
    @Column(name = "bundle_id")
    private Long bundleId;

    @Column(name = "style_variant_id")
    private Long styleVariantId;

    @Column(name = "routing_id")
    private Long routingId;

    @Column(name = "status")
    private String status;
}
