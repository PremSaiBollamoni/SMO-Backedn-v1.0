package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Individual defect type breakdown for a QC session.
 * Multiple defect types can be logged per QcLog entry.
 */
@Data
@Entity
@Table(name = "qc_defect",
        indexes = @Index(name = "idx_defect_qc", columnList = "qc_id"))
public class QcDefect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "defect_id")
    private Long defectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_id", nullable = false)
    private QcLog qcLog;

    @Column(name = "defect_type", nullable = false, length = 100)
    private String defectType;

    @Column(name = "count", nullable = false)
    private Integer count = 0;
}
