package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "routingstep")
public class RoutingStep {
    @Id
    @Column(name = "routing_step_id")
    private Long routingStepId;

    @Column(name = "routing_id")
    private Long routingId;

    @Column(name = "operation_id")
    private Long operationId;

    @Column(name = "stage_group")
    private Integer stageGroup;
}
