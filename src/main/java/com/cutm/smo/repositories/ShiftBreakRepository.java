package com.cutm.smo.repositories;

import com.cutm.smo.models.ShiftBreak;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShiftBreakRepository extends JpaRepository<ShiftBreak, Long> {
    List<ShiftBreak> findByShiftTemplateTemplateIdOrderByStartTimeAsc(Long templateId);
    void deleteByShiftTemplateTemplateId(Long templateId);
}
