package com.cutm.smo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NodeMetricsResponse {

    @JsonProperty("wip_count")
    private int wipCount;

    @JsonProperty("jobs_being_processed")
    private int jobsBeingProcessed;

    @JsonProperty("jobs_processed_today")
    private int jobsProcessedToday;

    public NodeMetricsResponse() {}

    public NodeMetricsResponse(int wipCount, int jobsBeingProcessed, int jobsProcessedToday) {
        this.wipCount = wipCount;
        this.jobsBeingProcessed = jobsBeingProcessed;
        this.jobsProcessedToday = jobsProcessedToday;
    }

    public int getWipCount() { return wipCount; }
    public void setWipCount(int wipCount) { this.wipCount = wipCount; }

    public int getJobsBeingProcessed() { return jobsBeingProcessed; }
    public void setJobsBeingProcessed(int jobsBeingProcessed) { this.jobsBeingProcessed = jobsBeingProcessed; }

    public int getJobsProcessedToday() { return jobsProcessedToday; }
    public void setJobsProcessedToday(int jobsProcessedToday) { this.jobsProcessedToday = jobsProcessedToday; }
}
