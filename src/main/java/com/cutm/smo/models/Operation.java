package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sewing operation master — 27 Sub Assembly + 7 Full Garment = 34 total.
 * zone: S1_PREP | S1_COLLAR | S1_CUFF | S1_BACK | S1_FRONT | S1_SLEEVE | S2
 */
@Data
@Entity
@Table(name = "operation",
        uniqueConstraints = @UniqueConstraint(name = "uk_op_code", columnNames = "op_code"))
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "op_id")
    private Long opId;

    @Column(name = "op_code", nullable = false, length = 20)
    private String opCode;

    @Column(name = "op_name", nullable = false, length = 150)
    private String opName;

    // S1_PREP | S1_COLLAR | S1_CUFF | S1_BACK | S1_FRONT | S1_SLEEVE | S2
    @Column(name = "stage", nullable = false, length = 30)
    private String stage;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    // SAM (minutes per piece)
    @Column(name = "sam", precision = 8, scale = 3)
    private BigDecimal sam;

    // A-grade | B-grade | C-grade | A+-grade | Helper
    @Column(name = "skill_grade", length = 20)
    private String skillGrade;

    // Target pieces per shift
    @Column(name = "target_pcs")
    private Integer targetPcs;

    // ACTIVE | INACTIVE
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
