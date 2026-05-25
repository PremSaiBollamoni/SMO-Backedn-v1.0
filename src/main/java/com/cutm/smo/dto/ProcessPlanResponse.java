package com.cutm.smo.dto;

import com.cutm.smo.models.OperationType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public class ProcessPlanResponse {
    @JsonProperty("routing_id")
    private Long routingId;
    @JsonProperty("product_id")
    private Long productId;
    private Integer version;
    private String status;
    @JsonProperty("approval_status")
    private String approvalStatus;
    @JsonProperty("approved_by")
    private Long approvedBy;
    @JsonProperty("approved_at")
    private LocalDateTime approvedAt;
    @JsonProperty("previous_routing_id")
    private Long previousRoutingId;
    private List<OperationResponse> operations;
    private List<WorkflowEdge> edges; // Explicit edges from routing table

    public Long getRoutingId() { return routingId; }
    public void setRoutingId(Long routingId) { this.routingId = routingId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public Long getPreviousRoutingId() { return previousRoutingId; }
    public void setPreviousRoutingId(Long previousRoutingId) { this.previousRoutingId = previousRoutingId; }
    public List<OperationResponse> getOperations() { return operations; }
    public void setOperations(List<OperationResponse> operations) { this.operations = operations; }
    public List<WorkflowEdge> getEdges() { return edges; }
    public void setEdges(List<WorkflowEdge> edges) { this.edges = edges; }

    public static class OperationResponse {
        @JsonProperty("operation_id")
        private Long operationId;
        private String name;
        private String description;
        private Integer sequence;
        @JsonProperty("operation_type")
        private OperationType operationType;
        @JsonProperty("stage_group")
        private Integer stageGroup;
        @JsonProperty("standard_time")
        private Integer standardTime;

        public Long getOperationId() { return operationId; }
        public void setOperationId(Long operationId) { this.operationId = operationId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getSequence() { return sequence; }
        public void setSequence(Integer sequence) { this.sequence = sequence; }
        public OperationType getOperationType() { return operationType; }
        public void setOperationType(OperationType operationType) { this.operationType = operationType; }
        public Integer getStageGroup() { return stageGroup; }
        public void setStageGroup(Integer stageGroup) { this.stageGroup = stageGroup; }
        public Integer getStandardTime() { return standardTime; }
        public void setStandardTime(Integer standardTime) { this.standardTime = standardTime; }
        
        /**
         * Get operation_type as string for JSON serialization
         * Ensures frontend receives lowercase string value
         */
        @JsonProperty("operation_type")
        public String getOperationTypeString() {
            return operationType != null ? operationType.name().toLowerCase() : "sequential";
        }
    }
}
