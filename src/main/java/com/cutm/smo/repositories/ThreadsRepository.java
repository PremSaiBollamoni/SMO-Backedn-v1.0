package com.cutm.smo.repositories;

import com.cutm.smo.models.Threads;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThreadsRepository extends JpaRepository<Threads, Long> {
    
    List<Threads> findByStatus(String status);
    
    @Query("SELECT COALESCE(MAX(t.threadId), 0) + 1 FROM Threads t")
    Long getNextThreadId();
}
