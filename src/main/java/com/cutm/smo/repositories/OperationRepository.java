package com.cutm.smo.repositories;

import com.cutm.smo.models.Operation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationRepository extends JpaRepository<Operation, Long> {
    @Query("select coalesce(max(o.operationId), 0) from Operation o")
    Long findMaxOperationId();
}
