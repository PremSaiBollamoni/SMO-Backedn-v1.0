package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "routing")
public class Routing {
    @Id
    @Column(name = "routing_id")
    private Long routingId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "version")
    private Integer version;

    @Column(name = "status")
    private String status;

    @Column(name = "approval_status")
    private String approvalStatus;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "previous_routing_id")
    private Long previousRoutingId;
}
