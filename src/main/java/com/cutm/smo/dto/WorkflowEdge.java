package com.cutm.smo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an explicit edge in the workflow DAG.
 * Derived from routing table relationships, not inferred from operation types.
 */
public class WorkflowEdge {
    @JsonProperty("from_operation_id")
    private Long fromOperationId;
    
    @JsonProperty("to_operation_id")
    private Long toOperationId;
    
    @JsonProperty("from_name")
    private String fromName;
    
    @JsonProperty("to_name")
    private String toName;
    
    @JsonProperty("edge_type")
    private String edgeType; // "sequential", "branch", "merge_convergence"

    public WorkflowEdge() {}

    public WorkflowEdge(Long fromOpId, Long toOpId, String fromName, String toName, String edgeType) {
        this.fromOperationId = fromOpId;
        this.toOperationId = toOpId;
        this.fromName = fromName;
        this.toName = toName;
        this.edgeType = edgeType;
    }

    public Long getFromOperationId() { return fromOperationId; }
    public void setFromOperationId(Long fromOperationId) { this.fromOperationId = fromOperationId; }
    
    public Long getToOperationId() { return toOperationId; }
    public void setToOperationId(Long toOperationId) { this.toOperationId = toOperationId; }
    
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    
    public String getToName() { return toName; }
    public void setToName(String toName) { this.toName = toName; }
    
    public String getEdgeType() { return edgeType; }
    public void setEdgeType(String edgeType) { this.edgeType = edgeType; }
}
