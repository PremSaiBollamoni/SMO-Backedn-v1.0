package com.cutm.smo.repositories;

import com.cutm.smo.models.EmployeeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, Long> {
    List<EmployeeRole> findByEmpId(Long empId);
    void deleteByEmpId(Long empId);
    boolean existsByEmpIdAndRoleId(Long empId, Long roleId);
}
