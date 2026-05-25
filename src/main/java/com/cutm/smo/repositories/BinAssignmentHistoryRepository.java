package com.cutm.smo.repositories;

import com.cutm.smo.models.BinAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BinAssignmentHistoryRepository extends JpaRepository<BinAssignmentHistory, Long> {
    
    /**
     * Find assignment history by bin ID
     */
    List<BinAssignmentHistory> findByBinIdOrderByCreatedAtDesc(Long binId);
    
    /**
     * Find assignment history by QR code
     */
    List<BinAssignmentHistory> findByQrCodeOrderByCreatedAtDesc(String qrCode);
    
    /**
     * Find assignment history by supervisor
     */
    List<BinAssignmentHistory> findByAssignedByOrderByCreatedAtDesc(Long assignedBy);
    
    /**
     * Find active assignments (no end time)
     */
    @Query("SELECT b FROM BinAssignmentHistory b WHERE b.assignmentEndTime IS NULL ORDER BY b.createdAt DESC")
    List<BinAssignmentHistory> findActiveAssignments();
    
    /**
     * Find assignment history within date range
     */
    @Query("SELECT b FROM BinAssignmentHistory b WHERE b.createdAt BETWEEN :startDate AND :endDate ORDER BY b.createdAt DESC")
    List<BinAssignmentHistory> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                             @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find assignment history by routing ID
     */
    List<BinAssignmentHistory> findByRoutingIdOrderByCreatedAtDesc(Long routingId);
    
    /**
     * Get next history ID
     */
    @Query("SELECT COALESCE(MAX(b.historyId), 0) + 1 FROM BinAssignmentHistory b")
    Long getNextHistoryId();
}