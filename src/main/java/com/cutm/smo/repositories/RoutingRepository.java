package com.cutm.smo.repositories;

import com.cutm.smo.models.Routing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutingRepository extends JpaRepository<Routing, Long> {
    @Query("select coalesce(max(r.routingId), 0) from Routing r")
    Long findMaxRoutingId();

    @Query("select coalesce(max(r.version), 0) from Routing r where r.productId = :productId")
    Integer findMaxVersionByProductId(@Param("productId") Long productId);

    List<Routing> findByProductIdOrderByRoutingIdDesc(Long productId);
    
    List<Routing> findByApprovalStatusOrderByRoutingIdDesc(String approvalStatus);

    List<Routing> findByStatusOrderByRoutingIdDesc(String status);
}
