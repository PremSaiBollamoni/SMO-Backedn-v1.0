package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "qc")
public class Qc {
    @Id
    @Column(name = "qc_id")
    private Long qcId;

    @Column(name = "garment_id")
    private Long garmentId;

    @Column(name = "status")
    private String status;

    @Column(name = "defects", columnDefinition = "TEXT")
    private String defects;
}
