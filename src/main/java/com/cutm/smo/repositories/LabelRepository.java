package com.cutm.smo.repositories;

import com.cutm.smo.models.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {
    
    List<Label> findByStatus(String status);
    
    @Query("SELECT COALESCE(MAX(l.labelId), 0) + 1 FROM Label l")
    Long getNextLabelId();
}
