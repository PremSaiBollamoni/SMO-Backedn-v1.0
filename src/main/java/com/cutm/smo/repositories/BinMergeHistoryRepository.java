package com.cutm.smo.repositories;

import com.cutm.smo.models.BinMergeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BinMergeHistoryRepository extends JpaRepository<BinMergeHistory, Long> {
    
    /**
     * Find merge history by source bin
     */
    List<BinMergeHistory> findBySourceBinIdOrderByMergedAtDesc(Long sourceBinId);
    
    /**
     * Find merge history by target bin
     */
    List<BinMergeHistory> findByTargetBinIdOrderByMergedAtDesc(Long targetBinId);
    
    /**
     * Find merge history by supervisor
     */
    List<BinMergeHistory> findByMergedByEmpIdOrderByMergedAtDesc(Long empId);
    
    /**
     * Find merge history within date range
     */
    @Query("SELECT b FROM BinMergeHistory b WHERE b.mergedAt BETWEEN :startDate AND :endDate ORDER BY b.mergedAt DESC")
    List<BinMergeHistory> findByMergedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get next merge ID
     */
    @Query("SELECT COALESCE(MAX(b.mergeId), 0) + 1 FROM BinMergeHistory b")
    Long getNextMergeId();
}