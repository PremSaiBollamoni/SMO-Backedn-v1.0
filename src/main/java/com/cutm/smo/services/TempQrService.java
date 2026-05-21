package com.cutm.smo.services;

import com.cutm.smo.dto.TempQrScanRequest;
import com.cutm.smo.dto.TempQrScanResponse;
import com.cutm.smo.models.EmployeeInfo;
import com.cutm.smo.models.QrScanHistory;
import com.cutm.smo.models.TempEmpQr;
import com.cutm.smo.repositories.EmployeeRepository;
import com.cutm.smo.repository.QrScanHistoryRepository;
import com.cutm.smo.repository.TempEmpQrRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TempQrService {
    
    @Autowired
    private TempEmpQrRepository tempEmpQrRepository;
    
    @Autowired
    private QrScanHistoryRepository qrScanHistoryRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Transactional
    public TempQrScanResponse handleQrScan(TempQrScanRequest request) {
        TempQrScanResponse response = new TempQrScanResponse();
        response.setQrId(request.getQrId());
        response.setScanTime(LocalDateTime.now());
        
        // Check if QR code already has an active mapping
        Optional<TempEmpQr> existingMapping = tempEmpQrRepository.findByQrIdAndStatus(
            request.getQrId(), "ACTIVE"
        );
        
        if (existingMapping.isPresent()) {
            // CHECK_OUT: Close the existing mapping
            TempEmpQr mapping = existingMapping.get();
            mapping.setEndTime(LocalDateTime.now());
            mapping.setStatus("COMPLETED");
            tempEmpQrRepository.save(mapping);
            
            // Record scan history
            QrScanHistory history = new QrScanHistory();
            history.setQrId(request.getQrId());
            history.setEmployeeId(mapping.getEmployeeId());
            history.setEmployeeName(mapping.getEmployeeName());
            history.setScanType("CHECK_OUT");
            history.setScannedBy(request.getScannedBy());
            history.setTempQrMappingId(mapping.getId());
            qrScanHistoryRepository.save(history);
            
            // Get employee name
            Optional<EmployeeInfo> employee = employeeRepository.findById(mapping.getEmployeeId());
            String employeeName = employee.map(EmployeeInfo::getEmpName).orElse("Unknown");
            
            response.setScanType("CHECK_OUT");
            response.setEmployeeId(mapping.getEmployeeId());
            response.setEmployeeName(employeeName);
            response.setMessage("Check-out successful for " + employeeName);
            response.setMappingId(mapping.getId());
            
        } else {
            // CHECK_IN: Create new mapping
            if (request.getEmployeeId() == null) {
                throw new IllegalArgumentException("Employee ID is required for check-in");
            }
            
            // Check if employee exists
            Optional<EmployeeInfo> employee = employeeRepository.findById(request.getEmployeeId());
            if (employee.isEmpty()) {
                throw new IllegalArgumentException("Employee not found: " + request.getEmployeeId());
            }
            
            // Check if employee already has an active mapping with another QR
            List<TempEmpQr> activeEmployeeMappings = tempEmpQrRepository.findByEmployeeIdAndStatus(
                request.getEmployeeId(), "ACTIVE"
            );
            if (!activeEmployeeMappings.isEmpty()) {
                throw new IllegalArgumentException(
                    "Employee already has an active QR mapping. Please check-out first."
                );
            }
            
            // Create new mapping
            TempEmpQr newMapping = new TempEmpQr();
            newMapping.setQrId(request.getQrId());
            newMapping.setEmployeeId(request.getEmployeeId());
            newMapping.setEmployeeName(employee.get().getEmpName());
            newMapping.setStartTime(LocalDateTime.now());
            newMapping.setStatus("ACTIVE");
            newMapping.setCreatedBy(request.getScannedBy());
            tempEmpQrRepository.save(newMapping);
            
            // Record scan history
            QrScanHistory history = new QrScanHistory();
            history.setQrId(request.getQrId());
            history.setEmployeeId(request.getEmployeeId());
            history.setEmployeeName(employee.get().getEmpName());
            history.setScanType("CHECK_IN");
            history.setScannedBy(request.getScannedBy());
            history.setTempQrMappingId(newMapping.getId());
            qrScanHistoryRepository.save(history);
            
            response.setScanType("CHECK_IN");
            response.setEmployeeId(request.getEmployeeId());
            response.setEmployeeName(employee.get().getEmpName());
            response.setMessage("Check-in successful for " + employee.get().getEmpName());
            response.setMappingId(newMapping.getId());
        }
        
        return response;
    }
    
    public List<TempEmpQr> getActiveMappings() {
        return tempEmpQrRepository.findByStatus("ACTIVE");
    }
    
    public List<TempEmpQr> getAllMappings() {
        return tempEmpQrRepository.findAllByOrderByCreatedAtDesc();
    }
    
    public List<QrScanHistory> getScanHistory() {
        return qrScanHistoryRepository.findAllByOrderByScanTimeDesc();
    }
    
    public List<QrScanHistory> getScanHistoryByQrId(String qrId) {
        return qrScanHistoryRepository.findByQrIdOrderByScanTimeDesc(qrId);
    }
    
    public List<EmployeeInfo> getAllEmployees() {
        return employeeRepository.findAll();
    }
    
    @Transactional
    public boolean unmapQrCode(Long mappingId, String unmappedBy) {
        Optional<TempEmpQr> mapping = tempEmpQrRepository.findById(mappingId);
        if (mapping.isPresent() && "ACTIVE".equals(mapping.get().getStatus())) {
            TempEmpQr tempQr = mapping.get();
            tempQr.setEndTime(LocalDateTime.now());
            tempQr.setStatus("COMPLETED");
            tempEmpQrRepository.save(tempQr);
            
            // Record manual unmap in history
            QrScanHistory history = new QrScanHistory();
            history.setQrId(tempQr.getQrId());
            history.setEmployeeId(tempQr.getEmployeeId());
            history.setEmployeeName(tempQr.getEmployeeName());
            history.setScanType("MANUAL_UNMAP");
            history.setScannedBy(unmappedBy);
            history.setTempQrMappingId(mappingId);
            qrScanHistoryRepository.save(history);
            
            return true;
        }
        return false;
    }
}
