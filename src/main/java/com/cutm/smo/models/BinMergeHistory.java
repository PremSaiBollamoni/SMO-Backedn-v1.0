package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bin_merge_history")
public class BinMergeHistory {
    @Id
    @Column(name = "merge_id")
    private Long mergeId;

    @Column(name = "source_bin_id")
    private Long sourceBinId;

    @Column(name = "target_bin_id")
    private Long targetBinId;

    @Column(name = "qty_transferred")
    private Integer qtyTransferred;

    @Column(name = "merged_by_emp_id")
    private Long mergedByEmpId;

    @Column(name = "merged_at")
    private LocalDateTime mergedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
