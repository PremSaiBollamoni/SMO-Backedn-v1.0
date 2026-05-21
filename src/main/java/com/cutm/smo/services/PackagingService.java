package com.cutm.smo.services;

import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import com.cutm.smo.util.LoggingUtil;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@Service
public class PackagingService {
    private final PackagingRepository packagingRepository;
    private final GarmentRepository garmentRepository;

    public PackagingService(PackagingRepository packagingRepository, GarmentRepository garmentRepository) {
        this.packagingRepository = packagingRepository;
        this.garmentRepository = garmentRepository;
    }

    public List<Packaging> getAllPackagingRecords() { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET ALL PACKAGING RECORDS START ===");
            List<Packaging> records = packagingRepository.findAll();
            log.info("Retrieved {} packaging records", records.size());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get All Packaging Records", startTime, endTime);
            log.info("=== GET ALL PACKAGING RECORDS END - SUCCESS ===");
            return records;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get all packaging records", e);
            LoggingUtil.logPerformance(log, "Get All Packaging Records (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public Packaging getPackagingById(Long id) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET PACKAGING BY ID START ===");
            log.debug("Packaging ID: {}", id);
            Packaging packaging = packagingRepository.findById(id).orElse(null);
            if (packaging != null) {
                log.info("Packaging record found with ID: {}", id);
            } else {
                log.warn("Packaging record not found with ID: {}", id);
            }
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Packaging By ID", startTime, endTime);
            log.info("=== GET PACKAGING BY ID END - SUCCESS ===");
            return packaging;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get packaging with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Get Packaging By ID (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public Packaging createPackaging(Packaging packaging) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE PACKAGING START ===");
            log.debug("Packaging Data: {}", packaging);
            Packaging saved = packagingRepository.save(packaging);
            log.info("Packaging record created successfully with ID: {}", saved.getPackagingId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create Packaging", startTime, endTime);
            log.info("=== CREATE PACKAGING END - SUCCESS ===");
            return saved;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to create packaging record", e);
            LoggingUtil.logPerformance(log, "Create Packaging (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public Packaging updatePackaging(Long id, Packaging packaging) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== UPDATE PACKAGING START ===");
            log.debug("Packaging ID: {}", id);
            log.debug("Packaging Data: {}", packaging);
            packaging.setPackagingId(id);
            Packaging updated = packagingRepository.save(packaging);
            log.info("Packaging record updated successfully with ID: {}", updated.getPackagingId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Update Packaging", startTime, endTime);
            log.info("=== UPDATE PACKAGING END - SUCCESS ===");
            return updated;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to update packaging with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Update Packaging (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public void deletePackaging(Long id) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== DELETE PACKAGING START ===");
            log.debug("Packaging ID to delete: {}", id);
            packagingRepository.deleteById(id);
            log.info("Packaging record deleted successfully with ID: {}", id);
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Delete Packaging", startTime, endTime);
            log.info("=== DELETE PACKAGING END - SUCCESS ===");
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to delete packaging with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Delete Packaging (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public List<Packaging> getPackagingByGarmentId(Long garmentId) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET PACKAGING BY GARMENT ID START ===");
            log.debug("Garment ID: {}", garmentId);
            List<Packaging> records = packagingRepository.findAll().stream()
                .filter(p -> p.getGarmentId() != null && p.getGarmentId().equals(garmentId))
                .toList();
            log.info("Retrieved {} packaging records for garment: {}", records.size(), garmentId);
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Packaging By Garment ID", startTime, endTime);
            log.info("=== GET PACKAGING BY GARMENT ID END - SUCCESS ===");
            return records;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get packaging records for garment: " + garmentId, e);
            LoggingUtil.logPerformance(log, "Get Packaging By Garment ID (Failed)", startTime, endTime);
            throw e;
        }
    }
}