package com.cutm.smo.repositories;

import com.cutm.smo.models.ShiftTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Long> {
    List<ShiftTemplate> findByStatusOrderByStartTimeAsc(String status);
}
