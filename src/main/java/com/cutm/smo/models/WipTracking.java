package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "wiptracking")
public class WipTracking {
    @Id
    @Column(name = "wip_id")
    private Long wipId;

    @Column(name = "bin_id")
    private Long binId;

    @Column(name = "bundle_id")
    private Long bundleId;

    @Column(name = "operation_id")
    private Long operationId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "qty")
    private Integer qty;

    @Column(name = "status")
    private String status;
}
