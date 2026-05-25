package com.cutm.smo.repositories;

import com.cutm.smo.models.RoutingStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutingStepRepository extends JpaRepository<RoutingStep, Long> {
    @Query("select coalesce(max(rs.routingStepId), 0) from RoutingStep rs")
    Long findMaxRoutingStepId();

    List<RoutingStep> findByRoutingIdOrderByRoutingStepIdAsc(Long routingId);
    
    List<RoutingStep> findByRoutingId(Long routingId);
}
