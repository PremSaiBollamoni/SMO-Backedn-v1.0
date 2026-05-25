package com.cutm.smo.repositories;

import com.cutm.smo.models.RoutingEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutingEdgeRepository extends JpaRepository<RoutingEdge, Long> {
    List<RoutingEdge> findByRoutingIdOrderByEdgeIdAsc(Long routingId);
    
    List<RoutingEdge> findByRoutingIdAndFromOperationId(Long routingId, Long fromOperationId);
    
    @Query("SELECT COALESCE(MAX(e.edgeId), 0) FROM RoutingEdge e")
    Long findMaxEdgeId();
    
    void deleteByRoutingId(Long routingId);
}
