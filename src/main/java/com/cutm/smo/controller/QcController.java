package com.cutm.smo.controller;

import com.cutm.smo.models.*;
import com.cutm.smo.services.*;
import com.cutm.smo.util.LoggingUtil;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/qc")
@CrossOrigin(origins = "*")
public class QcController {
    private final QcService qcService;

    public QcController(QcService qcService) { this.qcService = qcService; }

    @GetMapping
    public List<Qc> getAllQcRecords() {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET ALL QC RECORDS START ===");
            List<Qc> records = qcService.getAllQcRecords();
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

    @GetMapping("/{id}")
    public Qc getQcById(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET QC BY ID START ===");
            log.debug("QC ID: {}", id);
            Qc qc = qcService.getQcById(id);
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

    @PostMapping
    public Qc createQc(@RequestBody Qc qc) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE QC START ===");
            log.debug("QC Data: {}", qc);
            Qc createdQc = qcService.createQc(qc);
            log.info("QC record created successfully with ID: {}", createdQc.getQcId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create QC", startTime, endTime);
            log.info("=== CREATE QC END - SUCCESS ===");
            return createdQc;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to create QC record", e);
            LoggingUtil.logPerformance(log, "Create QC (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public Qc updateQc(@PathVariable Long id, @RequestBody Qc qc) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== UPDATE QC START ===");
            log.debug("QC ID: {}", id);
            log.debug("QC Data: {}", qc);
            Qc updatedQc = qcService.updateQc(id, qc);
            log.info("QC record updated successfully with ID: {}", updatedQc.getQcId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Update QC", startTime, endTime);
            log.info("=== UPDATE QC END - SUCCESS ===");
            return updatedQc;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to update QC with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Update QC (Failed)", startTime, endTime);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public void deleteQc(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== DELETE QC START ===");
            log.debug("QC ID to delete: {}", id);
            qcService.deleteQc(id);
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

    @GetMapping("/garment/{garmentId}")
    public List<Qc> getQcByGarmentId(@PathVariable Long garmentId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET QC BY GARMENT ID START ===");
            log.debug("Garment ID: {}", garmentId);
            List<Qc> records = qcService.getQcByGarmentId(garmentId);
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