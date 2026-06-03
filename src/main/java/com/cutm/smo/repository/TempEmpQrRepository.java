package com.cutm.smo.repository;

import com.cutm.smo.models.TempEmpQr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TempEmpQrRepository extends JpaRepository<TempEmpQr, Long> {
    
    Optional<TempEmpQr> findByQrIdAndStatus(String qrId, String status);
    
    List<TempEmpQr> findByStatus(String status);
    
    List<TempEmpQr> findByEmployeeIdAndStatus(Long employeeId, String status);
    
    List<TempEmpQr> findAllByOrderByCreatedAtDesc();
    
    List<TempEmpQr> findByOperationIdAndStatus(Long operationId, String status);
}
