package com.cutm.smo.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cutm.smo.models.EmployeeInfo;

public interface EmployeeInfoRepository extends JpaRepository<EmployeeInfo, Long> {
    Optional<EmployeeInfo> findByEmailIgnoreCase(String email);

    @Query("select coalesce(max(e.empId), 0) from EmployeeInfo e")
    Long findMaxEmpId();
    
    List<EmployeeInfo> findByRoleRoleId(Long roleId);
}
