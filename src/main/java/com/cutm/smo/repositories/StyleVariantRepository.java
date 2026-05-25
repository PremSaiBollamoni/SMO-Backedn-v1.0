package com.cutm.smo.repositories;

import com.cutm.smo.models.StyleVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StyleVariantRepository extends JpaRepository<StyleVariant, Long> {
    
    List<StyleVariant> findByStatus(String status);
    
    @Query("SELECT COALESCE(MAX(sv.styleVariantId), 0) + 1 FROM StyleVariant sv")
    Long getNextStyleVariantId();
}
