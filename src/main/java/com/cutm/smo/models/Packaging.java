package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "packaging")
public class Packaging {
    @Id
    @Column(name = "packaging_id")
    private Long packagingId;

    @Column(name = "garment_id")
    private Long garmentId;

    @Column(name = "qty")
    private Integer qty;

    @Column(name = "status")
    private String status;
}
