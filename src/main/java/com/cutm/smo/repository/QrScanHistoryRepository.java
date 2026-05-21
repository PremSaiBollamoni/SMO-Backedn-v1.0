package com.cutm.smo.repository;

import com.cutm.smo.models.QrScanHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QrScanHistoryRepository extends JpaRepository<QrScanHistory, Long> {
    
    List<QrScanHistory> findAllByOrderByScanTimeDesc();
    
    List<QrScanHistory> findByQrIdOrderByScanTimeDesc(String qrId);
    
    List<QrScanHistory> findByEmployeeIdOrderByScanTimeDesc(Long employeeId);
    
    List<QrScanHistory> findByScanTimeBetweenOrderByScanTimeDesc(LocalDateTime start, LocalDateTime end);
}
