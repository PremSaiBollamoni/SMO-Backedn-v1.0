package com.cutm.smo.repositories;

import com.cutm.smo.models.BreakWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BreakWindowRepository extends JpaRepository<BreakWindow, Long> {
    List<BreakWindow> findByIsActiveTrueOrderByBreakStartAsc();
}
