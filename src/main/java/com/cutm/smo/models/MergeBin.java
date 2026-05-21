package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "mergebin")
public class MergeBin {
    @Id
    @Column(name = "merge_bin_id")
    private Long mergeBinId;

    @Column(name = "bundle_id")
    private Long bundleId;
}
