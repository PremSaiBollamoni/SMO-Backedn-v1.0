package com.cutm.smo.repositories;

import com.cutm.smo.models.JobAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface JobAssignmentRepository extends JpaRepository<JobAssignment, Long> {

    Optional<JobAssignment> findByEmpIdAndStatus(Long empId, String status);

    List<JobAssignment> findByStatusOrderByStartTimeAsc(String status);

    @Query("SELECT j FROM JobAssignment j WHERE j.workstation.wsId = :wsId AND j.status = 'IN_PROGRESS'")
    Optional<JobAssignment> findActiveByWorkstation(Long wsId);

    @Query("SELECT j FROM JobAssignment j WHERE j.workstation.wsId = :wsId AND j.empId = :empId AND j.status = 'IN_PROGRESS'")
    Optional<JobAssignment> findActiveByStationAndEmp(Long wsId, Long empId);

    @Query("SELECT j FROM JobAssignment j WHERE j.workstation.wsId = :wsId AND j.status = 'IN_PROGRESS' ORDER BY j.startTime ASC")
    List<JobAssignment> findAllActiveByStation(Long wsId);
}
