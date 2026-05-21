package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "operation")
public class Operation {
    @Id
    @Column(name = "operation_id")
    private Long operationId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "sequence")
    private Integer sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'SEQUENTIAL'")
    private OperationType operationType = OperationType.SEQUENTIAL;

    @Column(name = "stage_group")
    private Integer stageGroup;

    @Column(name = "standard_time")
    private Integer standardTime;

    /**
     * Convenience method: Check if this is a parallel branch operation
     */
    public boolean isParallelBranch() {
        return operationType == OperationType.PARALLEL_BRANCH;
    }

    /**
     * Convenience method: Check if this is a merge operation
     */
    public boolean isMerge() {
        return operationType == OperationType.MERGE;
    }

    /**
     * Convenience method: Check if this is a sequential operation
     */
    public boolean isSequential() {
        return operationType == OperationType.SEQUENTIAL;
    }
}
