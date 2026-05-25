package com.cutm.smo.repositories;

import com.cutm.smo.models.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {
    
    List<Machine> findByStatus(String status);
    
    @Query("SELECT COALESCE(MAX(m.machineId), 0) + 1 FROM Machine m")
    Long getNextMachineId();
}
