package com.cutm.smo.repository;

import com.cutm.smo.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    List<Order> findByStatus(String status);
    
    List<Order> findByStatusOrderByCreatedAtDesc(String status);
    
    List<Order> findByProductId(Long productId);
    
    List<Order> findByRoutingId(Long routingId);
    
    @Query("SELECT o FROM Order o WHERE o.status IN ('ACTIVE', 'ON_HOLD') ORDER BY o.createdAt DESC")
    List<Order> findActiveOrders();
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") String status);
    
    boolean existsByOrderNumber(String orderNumber);
}
