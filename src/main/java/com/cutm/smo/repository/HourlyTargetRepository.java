package com.cutm.smo.repository;

import com.cutm.smo.models.HourlyTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HourlyTargetRepository extends JpaRepository<HourlyTarget, Long> {
    Optional<HourlyTarget> findByOperationId(Long operationId);
}
