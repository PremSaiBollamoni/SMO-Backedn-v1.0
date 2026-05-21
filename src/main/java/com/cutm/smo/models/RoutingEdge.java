package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "routing_edge")
public class RoutingEdge {
    @Id
    @Column(name = "edge_id")
    private Long edgeId;

    @Column(name = "routing_id", nullable = false)
    private Long routingId;

    @Column(name = "from_operation_id", nullable = false)
    private Long fromOperationId;

    @Column(name = "to_operation_id", nullable = false)
    private Long toOperationId;

    @Column(name = "from_name", nullable = false)
    private String fromName;

    @Column(name = "to_name", nullable = false)
    private String toName;

    @Column(name = "edge_type", nullable = false)
    private String edgeType;
}
