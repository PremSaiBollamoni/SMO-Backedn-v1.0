package com.cutm.smo.services;

import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import com.cutm.smo.util.LoggingUtil;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@Service
public class BinMergeService {
    private final MergeBinRepository mergeBinRepository;
    private final BinMergeHistoryRepository binMergeHistoryRepository;
    private final BinRepository binRepository;
    private final GarmentRepository garmentRepository;

    public BinMergeService(MergeBinRepository mergeBinRepository, BinMergeHistoryRepository binMergeHistoryRepository,
            BinRepository binRepository, GarmentRepository garmentRepository) {
        this.mergeBinRepository = mergeBinRepository;
        this.binMergeHistoryRepository = binMergeHistoryRepository;
        this.binRepository = binRepository;
        this.garmentRepository = garmentRepository;
    }

    public List<MergeBin> getAllMergeBins() { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET ALL MERGE BINS START ===");
            List<MergeBin> bins = mergeBinRepository.findAll();
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
    
    public MergeBin getMergeBinById(Long id) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET MERGE BIN BY ID START ===");
            log.debug("Merge Bin ID: {}", id);
            MergeBin bin = mergeBinRepository.findById(id).orElse(null);
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
    
    public MergeBin createMergeBin(MergeBin mergeBin) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE MERGE BIN START ===");
            log.debug("Merge Bin Data: {}", mergeBin);
            MergeBin saved = mergeBinRepository.save(mergeBin);
            log.info("Merge bin created successfully with ID: {}", saved.getMergeBinId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create Merge Bin", startTime, endTime);
            log.info("=== CREATE MERGE BIN END - SUCCESS ===");
            return saved;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to create merge bin", e);
            LoggingUtil.logPerformance(log, "Create Merge Bin (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public MergeBin updateMergeBin(Long id, MergeBin mergeBin) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== UPDATE MERGE BIN START ===");
            log.debug("Merge Bin ID: {}", id);
            log.debug("Merge Bin Data: {}", mergeBin);
            mergeBin.setMergeBinId(id);
            MergeBin updated = mergeBinRepository.save(mergeBin);
            log.info("Merge bin updated successfully with ID: {}", updated.getMergeBinId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Update Merge Bin", startTime, endTime);
            log.info("=== UPDATE MERGE BIN END - SUCCESS ===");
            return updated;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to update merge bin with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Update Merge Bin (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public void deleteMergeBin(Long id) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== DELETE MERGE BIN START ===");
            log.debug("Merge Bin ID to delete: {}", id);
            mergeBinRepository.deleteById(id);
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

    public List<BinMergeHistory> getAllMergeHistories() { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET ALL MERGE HISTORIES START ===");
            List<BinMergeHistory> histories = binMergeHistoryRepository.findAll();
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
    
    public BinMergeHistory createMergeHistory(BinMergeHistory history) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE MERGE HISTORY START ===");
            log.debug("Merge History Data: {}", history);
            BinMergeHistory saved = binMergeHistoryRepository.save(history);
            log.info("Merge history created successfully with ID: {}", saved.getMergeId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create Merge History", startTime, endTime);
            log.info("=== CREATE MERGE HISTORY END - SUCCESS ===");
            return saved;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to create merge history", e);
            LoggingUtil.logPerformance(log, "Create Merge History (Failed)", startTime, endTime);
            throw e;
        }
    }
}