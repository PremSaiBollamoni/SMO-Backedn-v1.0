package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "garment")
public class Garment {
    @Id
    @Column(name = "garment_id")
    private Long garmentId;

    @Column(name = "qr_code")
    private String qrCode;

    @Column(name = "merge_bin_id")
    private Long mergeBinId;

    @Column(name = "bundle_id")
    private Long bundleId;

    @Column(name = "bin_id")
    private Long binId;

    @Column(name = "style_variant_id")
    private Long styleVariantId;

    @Column(name = "status")
    private String status;
}
