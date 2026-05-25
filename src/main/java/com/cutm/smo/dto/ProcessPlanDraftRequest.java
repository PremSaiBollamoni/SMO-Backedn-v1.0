package com.cutm.smo.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Request body for POST /api/processplan/draft
 * Accepts both steps and explicit edges from the frontend.
 */
public class ProcessPlanDraftRequest {
    private List<Map<String, Object>> steps = new ArrayList<>();
    private List<EdgeRequest> edges = new ArrayList<>();

    public List<Map<String, Object>> getSteps() { return steps; }
    public void setSteps(List<Map<String, Object>> steps) { this.steps = steps; }

    public List<EdgeRequest> getEdges() { return edges; }
    public void setEdges(List<EdgeRequest> edges) { this.edges = edges; }

    public static class EdgeRequest {
        private String from_name;
        private String to_name;
        private String edge_type;

        public String getFrom_name() { return from_name; }
        public void setFrom_name(String from_name) { this.from_name = from_name; }

        public String getTo_name() { return to_name; }
        public void setTo_name(String to_name) { this.to_name = to_name; }

        public String getEdge_type() { return edge_type; }
        public void setEdge_type(String edge_type) { this.edge_type = edge_type; }
    }
}
