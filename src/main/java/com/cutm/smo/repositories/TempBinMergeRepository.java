package com.cutm.smo.repositories;

import com.cutm.smo.models.TempBinMerge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TempBinMergeRepository extends JpaRepository<TempBinMerge, Long> {
    
    /**
     * Find temp merges by supervisor
     */
    List<TempBinMerge> findByMergedByOrderByMergedAtDesc(Long mergedBy);
    
    /**
     * Find temp merges within date range
     */
    @Query("SELECT t FROM TempBinMerge t WHERE t.mergedAt BETWEEN :startDate AND :endDate ORDER BY t.mergedAt DESC")
    List<TempBinMerge> findByMergedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find temp merges by source or target bin QR
     */
    @Query("SELECT t FROM TempBinMerge t WHERE t.sourceBinQr = :qr OR t.targetBinQr = :qr ORDER BY t.mergedAt DESC")
    List<TempBinMerge> findByBinQr(@Param("qr") String qr);
}