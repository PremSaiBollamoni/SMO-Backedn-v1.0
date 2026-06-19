package com.cutm.smo.repositories;

import com.cutm.smo.models.Workstation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WorkstationRepository extends JpaRepository<Workstation, Long> {
    Optional<Workstation> findByMachineCode(String machineCode);
    Optional<Workstation> findByWsCode(String wsCode);
    List<Workstation> findByStatusOrderByWsCodeAsc(String status);

    @Query("SELECT w FROM Workstation w LEFT JOIN w.operation o WHERE w.status = 'ACTIVE' ORDER BY COALESCE(o.sequenceNo, 999), w.wsCode ASC")
    List<Workstation> findAllActiveOrderByOperationSequence();
}
