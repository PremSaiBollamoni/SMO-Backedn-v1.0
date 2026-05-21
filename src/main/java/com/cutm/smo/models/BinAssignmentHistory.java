package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bin_assignment_history")
public class BinAssignmentHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "bin_id", nullable = false)
    private Long binId;

    @Column(name = "qr_code", nullable = false)
    private String qrCode;

    @Column(name = "routing_id")
    private Long routingId;

    @Column(name = "style_variant_id")
    private Long styleVariantId;

    @Column(name = "size")
    private String size;

    @Column(name = "gtg_id")
    private String gtgId;

    @Column(name = "qty")
    private Integer qty;

    @Column(name = "assignment_start_time", nullable = false)
    private LocalDateTime assignmentStartTime;

    @Column(name = "assignment_end_time")
    private LocalDateTime assignmentEndTime;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @Column(name = "unassigned_by")
    private Long unassignedBy;

    @Column(name = "next_operation")
    private String nextOperation;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (assignmentStartTime == null) {
            assignmentStartTime = LocalDateTime.now();
        }
    }
}