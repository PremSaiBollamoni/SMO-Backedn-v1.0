package com.cutm.smo.services;

import com.cutm.smo.dto.NodeMetricsResponse;
import com.cutm.smo.models.Routing;
import com.cutm.smo.repositories.BinRepository;
import com.cutm.smo.repositories.RoutingRepository;
import com.cutm.smo.repositories.TempActiveAssignmentRepository;
import com.cutm.smo.repositories.WipTrackingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NodeMetricsService {

    private final BinRepository binRepository;
    private final TempActiveAssignmentRepository tempActiveAssignmentRepository;
    private final WipTrackingRepository wipTrackingRepository;
    private final RoutingRepository routingRepository;

    public NodeMetricsService(
            BinRepository binRepository,
            TempActiveAssignmentRepository tempActiveAssignmentRepository,
            WipTrackingRepository wipTrackingRepository,
            RoutingRepository routingRepository) {
        this.binRepository = binRepository;
        this.tempActiveAssignmentRepository = tempActiveAssignmentRepository;
        this.wipTrackingRepository = wipTrackingRepository;
        this.routingRepository = routingRepository;
    }

    public NodeMetricsResponse getNodeMetrics(Long routingId, Long operationId) {
        // Enforce: only APPROVED routings may expose node metrics
        Routing routing = routingRepository.findById(routingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Routing not found"));

        if (!"APPROVED".equalsIgnoreCase(routing.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Node metrics are only available for APPROVED process plans");
        }

        int wipCount = binRepository.countWipAtNode(routingId, operationId);
        int jobsBeingProcessed = tempActiveAssignmentRepository.countActiveJobsAtNode(routingId, operationId);
        int jobsProcessedToday = wipTrackingRepository.countCompletedTodayAtNode(routingId, operationId);
        return new NodeMetricsResponse(wipCount, jobsBeingProcessed, jobsProcessedToday);
    }
}
