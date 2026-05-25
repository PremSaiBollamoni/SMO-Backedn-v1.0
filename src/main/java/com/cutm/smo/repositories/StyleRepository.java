package com.cutm.smo.repositories;

import com.cutm.smo.models.Style;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StyleRepository extends JpaRepository<Style, Long> {
    
    List<Style> findByStatus(String status);
    
    @Query("SELECT COALESCE(MAX(s.styleId), 0) + 1 FROM Style s")
    Long getNextStyleId();
}
