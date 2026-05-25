package com.cutm.smo.repositories;

import com.cutm.smo.models.Bin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BinRepository extends JpaRepository<Bin, Long> {
    
    /**
     * Find bin by QR code
     */
    Optional<Bin> findByQrCode(String qrCode);
    
    /**
     * Find bins by current status
     */
    List<Bin> findByCurrentStatus(String currentStatus);
    
    /**
     * Find bins by current routing ID
     */
    List<Bin> findByCurrentRoutingId(Long currentRoutingId);
    
    /**
     * Find bins assigned by supervisor
     */
    List<Bin> findByLastAssignedBy(Long lastAssignedBy);
    
    /**
     * Check if QR code exists and get current status
     */
    @Query("SELECT b.currentStatus FROM Bin b WHERE b.qrCode = :qrCode")
    Optional<String> findCurrentStatusByQrCode(@Param("qrCode") String qrCode);
    
    /**
     * Get next bin ID
     */
    @Query("SELECT COALESCE(MAX(b.binId), 0) + 1 FROM Bin b")
    Long getNextBinId();
    
    /**
     * Find free bins (available for assignment)
     */
    @Query("SELECT b FROM Bin b WHERE b.currentStatus = 'free' OR b.currentStatus IS NULL ORDER BY b.createdAt DESC")
    List<Bin> findFreeBins();
    
    /**
     * Find assigned bins
     */
    @Query("SELECT b FROM Bin b WHERE b.currentStatus = 'assigned' ORDER BY b.assignmentStartTime DESC")
    List<Bin> findAssignedBins();

    /**
     * Count WIP bins at a specific routing + operation node (metric 1)
     */
    @Query("SELECT COUNT(b) FROM Bin b WHERE b.currentRoutingId = :routingId AND b.lastOperationId = :operationId AND b.currentStatus IN ('assigned', 'in_progress')")
    int countWipAtNode(@Param("routingId") Long routingId, @Param("operationId") Long operationId);
    
    /**
     * Find bins by order ID
     */
    List<Bin> findByOrderId(Long orderId);
}