package com.cutm.smo.repositories;

import com.cutm.smo.models.WipTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WipTrackingRepository extends JpaRepository<WipTracking, Long> {
    
    /**
     * Find maximum WIP tracking ID for generating new IDs
     */
    @Query("SELECT MAX(w.wipId) FROM WipTracking w")
    Long findMaxWipTrackingId();

    /**
     * Count completed jobs today at a specific routing + operation node (metric 3)
     */
    @Query("SELECT COUNT(w) FROM WipTracking w JOIN Bin b ON b.binId = w.binId WHERE w.operationId = :operationId AND b.currentRoutingId = :routingId AND FUNCTION('DATE', w.endTime) = CURRENT_DATE AND w.status = 'completed'")
    int countCompletedTodayAtNode(@Param("routingId") Long routingId, @Param("operationId") Long operationId);
}