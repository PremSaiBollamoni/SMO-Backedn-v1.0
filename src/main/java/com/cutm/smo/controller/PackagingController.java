package com.cutm.smo.controller;

import com.cutm.smo.models.*;
import com.cutm.smo.services.*;
import com.cutm.smo.util.LoggingUtil;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/packaging")
@CrossOrigin(origins = "*")
public class PackagingController {
    private final PackagingService packagingService;

    public PackagingController(PackagingService packagingService) { this.packagingService = packagingService; }

    @GetMapping
    public List<Packaging> getAllPackagingRecords() {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET ALL PACKAGING RECORDS START ===");
            List<Packaging> records = packagingService.getAllPackagingRecords();
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

    @GetMapping("/{id}")
    public Packaging getPackagingById(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET PACKAGING BY ID START ===");
            log.debug("Packaging ID: {}", id);
            Packaging packaging = packagingService.getPackagingById(id);
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

    @PostMapping
    public Packaging createPackaging(@RequestBody Packaging packaging) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE PACKAGING START ===");
            log.debug("Packaging Data: {}", packaging);
            Packaging createdPackaging = packagingService.createPackaging(packaging);
            log.info("Packaging record created successfully with ID: {}", createdPackaging.getPackagingId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create Packaging", startTime, endTime);
            log.info("=== CREATE PACKAGING END - SUCCESS ===");
            return createdPackaging;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to create packaging record", e);
            LoggingUtil.logPerformance(log, "Create Packaging (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public Packaging updatePackaging(@PathVariable Long id, @RequestBody Packaging packaging) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== UPDATE PACKAGING START ===");
            log.debug("Packaging ID: {}", id);
            log.debug("Packaging Data: {}", packaging);
            Packaging updatedPackaging = packagingService.updatePackaging(id, packaging);
            log.info("Packaging record updated successfully with ID: {}", updatedPackaging.getPackagingId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Update Packaging", startTime, endTime);
            log.info("=== UPDATE PACKAGING END - SUCCESS ===");
            return updatedPackaging;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to update packaging with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Update Packaging (Failed)", startTime, endTime);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public void deletePackaging(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== DELETE PACKAGING START ===");
            log.debug("Packaging ID to delete: {}", id);
            packagingService.deletePackaging(id);
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

    @GetMapping("/garment/{garmentId}")
    public List<Packaging> getPackagingByGarmentId(@PathVariable Long garmentId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET PACKAGING BY GARMENT ID START ===");
            log.debug("Garment ID: {}", garmentId);
            List<Packaging> records = packagingService.getPackagingByGarmentId(garmentId);
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