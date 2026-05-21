package com.cutm.smo.services;

import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import com.cutm.smo.util.LoggingUtil;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@Service
public class QcService {
    private final QcRepository qcRepository;
    private final GarmentRepository garmentRepository;

    public QcService(QcRepository qcRepository, GarmentRepository garmentRepository) {
        this.qcRepository = qcRepository;
        this.garmentRepository = garmentRepository;
    }

    public List<Qc> getAllQcRecords() { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET ALL QC RECORDS START ===");
            List<Qc> records = qcRepository.findAll();
            log.info("Retrieved {} QC records", records.size());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get All QC Records", startTime, endTime);
            log.info("=== GET ALL QC RECORDS END - SUCCESS ===");
            return records;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get all QC records", e);
            LoggingUtil.logPerformance(log, "Get All QC Records (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public Qc getQcById(Long id) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET QC BY ID START ===");
            log.debug("QC ID: {}", id);
            Qc qc = qcRepository.findById(id).orElse(null);
            if (qc != null) {
                log.info("QC record found with ID: {}", id);
            } else {
                log.warn("QC record not found with ID: {}", id);
            }
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get QC By ID", startTime, endTime);
            log.info("=== GET QC BY ID END - SUCCESS ===");
            return qc;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get QC with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Get QC By ID (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public Qc createQc(Qc qc) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE QC START ===");
            log.debug("QC Data: {}", qc);
            Qc saved = qcRepository.save(qc);
            log.info("QC record created successfully with ID: {}", saved.getQcId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create QC", startTime, endTime);
            log.info("=== CREATE QC END - SUCCESS ===");
            return saved;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to create QC record", e);
            LoggingUtil.logPerformance(log, "Create QC (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public Qc updateQc(Long id, Qc qc) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== UPDATE QC START ===");
            log.debug("QC ID: {}", id);
            log.debug("QC Data: {}", qc);
            qc.setQcId(id);
            Qc updated = qcRepository.save(qc);
            log.info("QC record updated successfully with ID: {}", updated.getQcId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Update QC", startTime, endTime);
            log.info("=== UPDATE QC END - SUCCESS ===");
            return updated;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to update QC with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Update QC (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public void deleteQc(Long id) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== DELETE QC START ===");
            log.debug("QC ID to delete: {}", id);
            qcRepository.deleteById(id);
            log.info("QC record deleted successfully with ID: {}", id);
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Delete QC", startTime, endTime);
            log.info("=== DELETE QC END - SUCCESS ===");
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to delete QC with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Delete QC (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public List<Qc> getQcByGarmentId(Long garmentId) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET QC BY GARMENT ID START ===");
            log.debug("Garment ID: {}", garmentId);
            List<Qc> records = qcRepository.findAll().stream()
                .filter(q -> q.getGarmentId() != null && q.getGarmentId().equals(garmentId))
                .toList();
            log.info("Retrieved {} QC records for garment: {}", records.size(), garmentId);
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get QC By Garment ID", startTime, endTime);
            log.info("=== GET QC BY GARMENT ID END - SUCCESS ===");
            return records;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get QC records for garment: " + garmentId, e);
            LoggingUtil.logPerformance(log, "Get QC By Garment ID (Failed)", startTime, endTime);
            throw e;
        }
    }
}