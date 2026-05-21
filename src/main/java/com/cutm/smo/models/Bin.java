package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bin")
public class Bin {
    @Id
    @Column(name = "bin_id")
    private Long binId;

    @Column(name = "qr_code")
    private String qrCode;

    @Column(name = "style_id")
    private Long styleId;

    @Column(name = "style_variant_id")
    private Long styleVariantId;

    @Column(name = "size")
    private String size;

    @Column(name = "sleeve_type")
    private String sleeveType;

    @Column(name = "qty")
    private Integer qty;

    @Column(name = "status")
    private String status;

    @Column(name = "parent_bin_id")
    private Long parentBinId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Enhanced QR Assignment fields
    @Column(name = "current_routing_id")
    private Long currentRoutingId;

    @Column(name = "current_style_variant_id")
    private Long currentStyleVariantId;

    @Column(name = "assignment_start_time")
    private LocalDateTime assignmentStartTime;

    @Column(name = "assignment_end_time")
    private LocalDateTime assignmentEndTime;

    @Column(name = "last_assigned_by")
    private Long lastAssignedBy;

    @Column(name = "current_status")
    private String currentStatus; // 'free', 'assigned', 'in_progress', 'completed'

    @Column(name = "last_operation_id")
    private Long lastOperationId;

    @Column(name = "current_operation_id")
    private Long currentOperationId;

    @Column(name = "order_id")
    private Long orderId;
}
