package com.cutm.smo.controller;

import com.cutm.smo.models.*;
import com.cutm.smo.services.*;
import com.cutm.smo.util.LoggingUtil;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/store")
@CrossOrigin(origins = "*")
public class StoreController {
    private final StoreService storeService;

    public StoreController(StoreService storeService) { this.storeService = storeService; }
    
    private <T> T executeWithLogging(String operationName, String entityType, Long id, java.util.function.Supplier<T> operation) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== {} START ===", operationName);
            if (id != null) log.debug("{} ID: {}", entityType, id);
            T result = operation.get();
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, operationName, startTime, endTime);
            log.info("=== {} END - SUCCESS ===", operationName);
            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to execute " + operationName, e);
            LoggingUtil.logPerformance(log, operationName + " (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping("/items")
    public List<Item> getAllItems() { 
        return executeWithLogging("GET ALL ITEMS", "Item", null, () -> storeService.getAllItems()); 
    }
    @GetMapping("/items/{id}")
    public Item getItemById(@PathVariable Long id) { 
        return executeWithLogging("GET ITEM BY ID", "Item", id, () -> storeService.getItemById(id)); 
    }
    @PostMapping("/items")
    public Item createItem(@RequestBody Item item) { 
        return executeWithLogging("CREATE ITEM", "Item", null, () -> storeService.createItem(item)); 
    }
    @PutMapping("/items/{id}")
    public Item updateItem(@PathVariable Long id, @RequestBody Item item) { 
        return executeWithLogging("UPDATE ITEM", "Item", id, () -> storeService.updateItem(id, item)); 
    }
    @DeleteMapping("/items/{id}")
    public void deleteItem(@PathVariable Long id) { 
        executeWithLogging("DELETE ITEM", "Item", id, () -> { storeService.deleteItem(id); return null; }); 
    }

    @GetMapping("/vendors")
    public List<Vendor> getAllVendors() { 
        return executeWithLogging("GET ALL VENDORS", "Vendor", null, () -> storeService.getAllVendors()); 
    }
    @GetMapping("/vendors/{id}")
    public Vendor getVendorById(@PathVariable Long id) { 
        return executeWithLogging("GET VENDOR BY ID", "Vendor", id, () -> storeService.getVendorById(id)); 
    }
    @PostMapping("/vendors")
    public Vendor createVendor(@RequestBody Vendor vendor) { 
        return executeWithLogging("CREATE VENDOR", "Vendor", null, () -> storeService.createVendor(vendor)); 
    }
    @PutMapping("/vendors/{id}")
    public Vendor updateVendor(@PathVariable Long id, @RequestBody Vendor vendor) { 
        return executeWithLogging("UPDATE VENDOR", "Vendor", id, () -> storeService.updateVendor(id, vendor)); 
    }
    @DeleteMapping("/vendors/{id}")
    public void deleteVendor(@PathVariable Long id) { 
        executeWithLogging("DELETE VENDOR", "Vendor", id, () -> { storeService.deleteVendor(id); return null; }); 
    }

    @GetMapping("/purchaseorders")
    public List<PurchaseOrder> getAllPurchaseOrders() { 
        return executeWithLogging("GET ALL PURCHASE ORDERS", "PurchaseOrder", null, () -> storeService.getAllPurchaseOrders()); 
    }
    @GetMapping("/purchaseorders/{id}")
    public PurchaseOrder getPurchaseOrderById(@PathVariable Long id) { 
        return executeWithLogging("GET PURCHASE ORDER BY ID", "PurchaseOrder", id, () -> storeService.getPurchaseOrderById(id)); 
    }
    @PostMapping("/purchaseorders")
    public PurchaseOrder createPurchaseOrder(@RequestBody PurchaseOrder po) { 
        return executeWithLogging("CREATE PURCHASE ORDER", "PurchaseOrder", null, () -> storeService.createPurchaseOrder(po)); 
    }
    @PutMapping("/purchaseorders/{id}")
    public PurchaseOrder updatePurchaseOrder(@PathVariable Long id, @RequestBody PurchaseOrder po) { 
        return executeWithLogging("UPDATE PURCHASE ORDER", "PurchaseOrder", id, () -> storeService.updatePurchaseOrder(id, po)); 
    }
    @DeleteMapping("/purchaseorders/{id}")
    public void deletePurchaseOrder(@PathVariable Long id) { 
        executeWithLogging("DELETE PURCHASE ORDER", "PurchaseOrder", id, () -> { storeService.deletePurchaseOrder(id); return null; }); 
    }

    @GetMapping("/poitems")
    public List<PoItems> getAllPoItems() { 
        return executeWithLogging("GET ALL PO ITEMS", "PoItems", null, () -> storeService.getAllPoItems()); 
    }
    @GetMapping("/poitems/{id}")
    public PoItems getPoItemsById(@PathVariable Long id) { 
        return executeWithLogging("GET PO ITEMS BY ID", "PoItems", id, () -> storeService.getPoItemsById(id)); 
    }
    @PostMapping("/poitems")
    public PoItems createPoItems(@RequestBody PoItems items) { 
        return executeWithLogging("CREATE PO ITEMS", "PoItems", null, () -> storeService.createPoItems(items)); 
    }
    @PutMapping("/poitems/{id}")
    public PoItems updatePoItems(@PathVariable Long id, @RequestBody PoItems items) { 
        return executeWithLogging("UPDATE PO ITEMS", "PoItems", id, () -> storeService.updatePoItems(id, items)); 
    }
    @DeleteMapping("/poitems/{id}")
    public void deletePoItems(@PathVariable Long id) { 
        executeWithLogging("DELETE PO ITEMS", "PoItems", id, () -> { storeService.deletePoItems(id); return null; }); 
    }

    @GetMapping("/grns")
    public List<Grn> getAllGrns() { 
        return executeWithLogging("GET ALL GRNS", "Grn", null, () -> storeService.getAllGrns()); 
    }
    @GetMapping("/grns/{id}")
    public Grn getGrnById(@PathVariable Long id) { 
        return executeWithLogging("GET GRN BY ID", "Grn", id, () -> storeService.getGrnById(id)); 
    }
    @PostMapping("/grns")
    public Grn createGrn(@RequestBody Grn grn) { 
        return executeWithLogging("CREATE GRN", "Grn", null, () -> storeService.createGrn(grn)); 
    }
    @PutMapping("/grns/{id}")
    public Grn updateGrn(@PathVariable Long id, @RequestBody Grn grn) { 
        return executeWithLogging("UPDATE GRN", "Grn", id, () -> storeService.updateGrn(id, grn)); 
    }
    @DeleteMapping("/grns/{id}")
    public void deleteGrn(@PathVariable Long id) { 
        executeWithLogging("DELETE GRN", "Grn", id, () -> { storeService.deleteGrn(id); return null; }); 
    }

    @GetMapping("/grnitems")
    public List<GrnItems> getAllGrnItems() { 
        return executeWithLogging("GET ALL GRN ITEMS", "GrnItems", null, () -> storeService.getAllGrnItems()); 
    }
    @GetMapping("/grnitems/{id}")
    public GrnItems getGrnItemsById(@PathVariable Long id) { 
        return executeWithLogging("GET GRN ITEMS BY ID", "GrnItems", id, () -> storeService.getGrnItemsById(id)); 
    }
    @PostMapping("/grnitems")
    public GrnItems createGrnItems(@RequestBody GrnItems items) { 
        return executeWithLogging("CREATE GRN ITEMS", "GrnItems", null, () -> storeService.createGrnItems(items)); 
    }
    @PutMapping("/grnitems/{id}")
    public GrnItems updateGrnItems(@PathVariable Long id, @RequestBody GrnItems items) { 
        return executeWithLogging("UPDATE GRN ITEMS", "GrnItems", id, () -> storeService.updateGrnItems(id, items)); 
    }
    @DeleteMapping("/grnitems/{id}")
    public void deleteGrnItems(@PathVariable Long id) { 
        executeWithLogging("DELETE GRN ITEMS", "GrnItems", id, () -> { storeService.deleteGrnItems(id); return null; }); 
    }

    @GetMapping("/inventory")
    public List<InventoryStock> getAllInventoryStocks() { 
        return executeWithLogging("GET ALL INVENTORY STOCKS", "InventoryStock", null, () -> storeService.getAllInventoryStocks()); 
    }
    @GetMapping("/inventory/{id}")
    public InventoryStock getInventoryStockById(@PathVariable Long id) { 
        return executeWithLogging("GET INVENTORY STOCK BY ID", "InventoryStock", id, () -> storeService.getInventoryStockById(id)); 
    }
    @PostMapping("/inventory")
    public InventoryStock createInventoryStock(@RequestBody InventoryStock stock) { 
        return executeWithLogging("CREATE INVENTORY STOCK", "InventoryStock", null, () -> storeService.createInventoryStock(stock)); 
    }
    @PutMapping("/inventory/{id}")
    public InventoryStock updateInventoryStock(@PathVariable Long id, @RequestBody InventoryStock stock) { 
        return executeWithLogging("UPDATE INVENTORY STOCK", "InventoryStock", id, () -> storeService.updateInventoryStock(id, stock)); 
    }
    @DeleteMapping("/inventory/{id}")
    public void deleteInventoryStock(@PathVariable Long id) { 
        executeWithLogging("DELETE INVENTORY STOCK", "InventoryStock", id, () -> { storeService.deleteInventoryStock(id); return null; }); 
    }

    @GetMapping("/stockmovements")
    public List<StockMovement> getAllStockMovements() { 
        return executeWithLogging("GET ALL STOCK MOVEMENTS", "StockMovement", null, () -> storeService.getAllStockMovements()); 
    }
    @PostMapping("/stockmovements")
    public StockMovement createStockMovement(@RequestBody StockMovement movement) { 
        return executeWithLogging("CREATE STOCK MOVEMENT", "StockMovement", null, () -> storeService.createStockMovement(movement)); 
    }

    @GetMapping("/boms")
    public List<Bom> getAllBoms() { 
        return executeWithLogging("GET ALL BOMS", "Bom", null, () -> storeService.getAllBoms()); 
    }
    @GetMapping("/boms/{id}")
    public Bom getBomById(@PathVariable Long id) { 
        return executeWithLogging("GET BOM BY ID", "Bom", id, () -> storeService.getBomById(id)); 
    }
    @GetMapping("/boms/stylevariant/{styleVariantId}")
    public List<Bom> getBomsByStyleVariantId(@PathVariable Long styleVariantId) { 
        return executeWithLogging("GET BOMS BY STYLE VARIANT ID", "Bom", styleVariantId, () -> storeService.getBomsByStyleVariantId(styleVariantId)); 
    }
    @PostMapping("/boms")
    public Bom createBom(@RequestBody Bom bom) { 
        return executeWithLogging("CREATE BOM", "Bom", null, () -> storeService.createBom(bom)); 
    }
    @PutMapping("/boms/{id}")
    public Bom updateBom(@PathVariable Long id, @RequestBody Bom bom) { 
        return executeWithLogging("UPDATE BOM", "Bom", id, () -> storeService.updateBom(id, bom)); 
    }
    @DeleteMapping("/boms/{id}")
    public void deleteBom(@PathVariable Long id) { 
        executeWithLogging("DELETE BOM", "Bom", id, () -> { storeService.deleteBom(id); return null; }); 
    }
}