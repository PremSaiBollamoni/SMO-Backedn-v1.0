package com.cutm.smo.controller;

import com.cutm.smo.models.*;
import com.cutm.smo.services.*;
import com.cutm.smo.util.LoggingUtil;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/binmerge")
@CrossOrigin(origins = "*")
public class BinMergeController {
    private final BinMergeService binMergeService;

    public BinMergeController(BinMergeService binMergeService) { this.binMergeService = binMergeService; }

    @GetMapping
    public List<MergeBin> getAllMergeBins() {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET ALL MERGE BINS START ===");
            List<MergeBin> bins = binMergeService.getAllMergeBins();
            log.info("Retrieved {} merge bins", bins.size());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get All Merge Bins", startTime, endTime);
            log.info("=== GET ALL MERGE BINS END - SUCCESS ===");
            return bins;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get all merge bins", e);
            LoggingUtil.logPerformance(log, "Get All Merge Bins (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public MergeBin getMergeBinById(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET MERGE BIN BY ID START ===");
            log.debug("Merge Bin ID: {}", id);
            MergeBin bin = binMergeService.getMergeBinById(id);
            if (bin != null) {
                log.info("Merge bin found with ID: {}", id);
            } else {
                log.warn("Merge bin not found with ID: {}", id);
            }
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Merge Bin By ID", startTime, endTime);
            log.info("=== GET MERGE BIN BY ID END - SUCCESS ===");
            return bin;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get merge bin with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Get Merge Bin By ID (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PostMapping
    public MergeBin createMergeBin(@RequestBody MergeBin mergeBin) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE MERGE BIN START ===");
            log.debug("Merge Bin Data: {}", mergeBin);
            MergeBin createdBin = binMergeService.createMergeBin(mergeBin);
            log.info("Merge bin created successfully with ID: {}", createdBin.getMergeBinId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create Merge Bin", startTime, endTime);
            log.info("=== CREATE MERGE BIN END - SUCCESS ===");
            return createdBin;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to create merge bin", e);
            LoggingUtil.logPerformance(log, "Create Merge Bin (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public MergeBin updateMergeBin(@PathVariable Long id, @RequestBody MergeBin mergeBin) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== UPDATE MERGE BIN START ===");
            log.debug("Merge Bin ID: {}", id);
            log.debug("Merge Bin Data: {}", mergeBin);
            MergeBin updatedBin = binMergeService.updateMergeBin(id, mergeBin);
            log.info("Merge bin updated successfully with ID: {}", updatedBin.getMergeBinId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Update Merge Bin", startTime, endTime);
            log.info("=== UPDATE MERGE BIN END - SUCCESS ===");
            return updatedBin;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to update merge bin with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Update Merge Bin (Failed)", startTime, endTime);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public void deleteMergeBin(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== DELETE MERGE BIN START ===");
            log.debug("Merge Bin ID to delete: {}", id);
            binMergeService.deleteMergeBin(id);
            log.info("Merge bin deleted successfully with ID: {}", id);
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Delete Merge Bin", startTime, endTime);
            log.info("=== DELETE MERGE BIN END - SUCCESS ===");
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to delete merge bin with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Delete Merge Bin (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping("/history")
    public List<BinMergeHistory> getAllMergeHistories() {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET ALL MERGE HISTORIES START ===");
            List<BinMergeHistory> histories = binMergeService.getAllMergeHistories();
            log.info("Retrieved {} merge histories", histories.size());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get All Merge Histories", startTime, endTime);
            log.info("=== GET ALL MERGE HISTORIES END - SUCCESS ===");
            return histories;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get all merge histories", e);
            LoggingUtil.logPerformance(log, "Get All Merge Histories (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PostMapping("/history")
    public BinMergeHistory createMergeHistory(@RequestBody BinMergeHistory history) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE MERGE HISTORY START ===");
            log.debug("Merge History Data: {}", history);
            BinMergeHistory createdHistory = binMergeService.createMergeHistory(history);
            log.info("Merge history created successfully with ID: {}", createdHistory.getMergeId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create Merge History", startTime, endTime);
            log.info("=== CREATE MERGE HISTORY END - SUCCESS ===");
            return createdHistory;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to create merge history", e);
            LoggingUtil.logPerformance(log, "Create Merge History (Failed)", startTime, endTime);
            throw e;
        }
    }
}