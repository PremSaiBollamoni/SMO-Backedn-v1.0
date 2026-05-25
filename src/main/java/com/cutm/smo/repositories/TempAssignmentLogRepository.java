package com.cutm.smo.repositories;

import com.cutm.smo.models.TempAssignmentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TempAssignmentLogRepository extends JpaRepository<TempAssignmentLog, Long> {
    
    /**
     * Find maximum log ID for generating new IDs if needed
     */
    @Query("SELECT MAX(t.logId) FROM TempAssignmentLog t")
    Long findMaxLogId();
}