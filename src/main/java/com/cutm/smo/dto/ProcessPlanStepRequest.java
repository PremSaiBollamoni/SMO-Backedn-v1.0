package com.cutm.smo.dto;

import com.cutm.smo.models.OperationType;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProcessPlanStepRequest {
    private String name;
    private String description;
    private Integer sequence;
    @JsonProperty("operation_type")
    private OperationType operationType;
    @JsonProperty("stage_group")
    private Integer stageGroup;
    @JsonProperty("standard_time")
    private Integer standardTime;

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
}
