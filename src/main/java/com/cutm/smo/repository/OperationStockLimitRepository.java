package com.cutm.smo.repository;

import com.cutm.smo.models.OperationStockLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OperationStockLimitRepository extends JpaRepository<OperationStockLimit, Long> {
    Optional<OperationStockLimit> findByOperationId(Long operationId);
    void deleteByOperationId(Long operationId);
}
