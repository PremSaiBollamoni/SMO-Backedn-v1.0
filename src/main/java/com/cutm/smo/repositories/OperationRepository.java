package com.cutm.smo.repositories;

import com.cutm.smo.models.Operation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OperationRepository extends JpaRepository<Operation, Long> {
    List<Operation> findByStatusOrderBySequenceNoAsc(String status);
    Optional<Operation> findByOpCode(String opCode);
    Optional<Operation> findByOpNameIgnoreCase(String opName);
}
