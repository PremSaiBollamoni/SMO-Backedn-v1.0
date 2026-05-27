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
     * Counts wiptracking records that are completed today at this operation
     */
    @Query("SELECT COUNT(w) FROM WipTracking w WHERE w.operationId = :operationId AND CAST(w.endTime AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE) AND LOWER(w.status) = 'completed'")
    int countCompletedTodayAtNode(@Param("routingId") Long routingId, @Param("operationId") Long operationId);
}