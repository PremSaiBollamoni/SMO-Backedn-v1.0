package com.cutm.smo.dto;

import java.util.List;

public class ProcessPlanRequest {
    private Long productId;
    private Long routingId;
    private Integer version;
    private String status;
    private List<OperationPlan> operations;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getRoutingId() { return routingId; }
    public void setRoutingId(Long routingId) { this.routingId = routingId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<OperationPlan> getOperations() { return operations; }
    public void setOperations(List<OperationPlan> operations) { this.operations = operations; }

    public static class OperationPlan {
        private String name;
        private String description;
        private Integer sequence;
        private Boolean isParallel;
        private Boolean mergePoint;
        private Integer stageGroup;
        private Integer standardTime;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getSequence() { return sequence; }
        public void setSequence(Integer sequence) { this.sequence = sequence; }
        public Boolean getIsParallel() { return isParallel; }
        public void setIsParallel(Boolean isParallel) { this.isParallel = isParallel; }
        public Boolean getMergePoint() { return mergePoint; }
        public void setMergePoint(Boolean mergePoint) { this.mergePoint = mergePoint; }
        public Integer getStageGroup() { return stageGroup; }
        public void setStageGroup(Integer stageGroup) { this.stageGroup = stageGroup; }
        public Integer getStandardTime() { return standardTime; }
        public void setStandardTime(Integer standardTime) { this.standardTime = standardTime; }
    }
}