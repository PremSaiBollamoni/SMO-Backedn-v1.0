package com.cutm.smo.repositories;

import com.cutm.smo.models.ProductionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductionLogRepository extends JpaRepository<ProductionLog, Long> {

    @Query("SELECT p FROM ProductionLog p WHERE p.workstation.wsId = :wsId AND CAST(p.loggedAt AS date) = :date")
    List<ProductionLog> findByWsIdAndDate(@Param("wsId") Long wsId, @Param("date") LocalDate date);

    @Query("SELECT p FROM ProductionLog p WHERE CAST(p.loggedAt AS date) = :date")
    List<ProductionLog> findAllByDate(@Param("date") LocalDate date);
}
