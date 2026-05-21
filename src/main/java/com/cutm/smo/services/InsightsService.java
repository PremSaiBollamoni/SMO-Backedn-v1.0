package com.cutm.smo.services;

import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import com.cutm.smo.util.LoggingUtil;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@Service
public class InsightsService {
    private final GrnRepository grnRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final WipTrackingRepository wipTrackingRepository;

    public InsightsService(GrnRepository grnRepository, PurchaseOrderRepository purchaseOrderRepository,
            InventoryStockRepository inventoryStockRepository, WipTrackingRepository wipTrackingRepository) {
        this.grnRepository = grnRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.inventoryStockRepository = inventoryStockRepository;
        this.wipTrackingRepository = wipTrackingRepository;
    }

    public long getTotalPurchaseOrders() { 
        long startTime = System.currentTimeMillis();
        try {
            log.debug("=== GET TOTAL PURCHASE ORDERS START ===");
            long count = purchaseOrderRepository.count();
            log.debug("Total Purchase Orders: {}", count);
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Total Purchase Orders", startTime, endTime);
            log.debug("=== GET TOTAL PURCHASE ORDERS END - SUCCESS ===");
            return count;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get total purchase orders", e);
            LoggingUtil.logPerformance(log, "Get Total Purchase Orders (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public long getTotalGrns() { 
        long startTime = System.currentTimeMillis();
        try {
            log.debug("=== GET TOTAL GRNS START ===");
            long count = grnRepository.count();
            log.debug("Total GRNs: {}", count);
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Total GRNs", startTime, endTime);
            log.debug("=== GET TOTAL GRNS END - SUCCESS ===");
            return count;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get total GRNs", e);
            LoggingUtil.logPerformance(log, "Get Total GRNs (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public long getTotalInventoryItems() { 
        long startTime = System.currentTimeMillis();
        try {
            log.debug("=== GET TOTAL INVENTORY ITEMS START ===");
            long count = inventoryStockRepository.count();
            log.debug("Total Inventory Items: {}", count);
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Total Inventory Items", startTime, endTime);
            log.debug("=== GET TOTAL INVENTORY ITEMS END - SUCCESS ===");
            return count;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get total inventory items", e);
            LoggingUtil.logPerformance(log, "Get Total Inventory Items (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public long getTotalWipRecords() { 
        long startTime = System.currentTimeMillis();
        try {
            log.debug("=== GET TOTAL WIP RECORDS START ===");
            long count = wipTrackingRepository.count();
            log.debug("Total WIP Records: {}", count);
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Total WIP Records", startTime, endTime);
            log.debug("=== GET TOTAL WIP RECORDS END - SUCCESS ===");
            return count;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get total WIP records", e);
            LoggingUtil.logPerformance(log, "Get Total WIP Records (Failed)", startTime, endTime);
            throw e;
        }
    }
}