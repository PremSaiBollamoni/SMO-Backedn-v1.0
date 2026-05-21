package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "temp_bin_merges")
public class TempBinMerge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merge_temp_id")
    private Long mergeTempId;

    @Column(name = "source_bin_qr", nullable = false)
    private String sourceBinQr;

    @Column(name = "target_bin_qr", nullable = false)
    private String targetBinQr;

    @Column(name = "source_bin_id")
    private Long sourceBinId;

    @Column(name = "target_bin_id")
    private Long targetBinId;

    @Column(name = "qty_transferred", nullable = false)
    private Integer qtyTransferred;

    @Column(name = "merged_by")
    private Long mergedBy;

    @Column(name = "merged_at")
    private LocalDateTime mergedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (mergedAt == null) {
            mergedAt = LocalDateTime.now();
        }
    }
}