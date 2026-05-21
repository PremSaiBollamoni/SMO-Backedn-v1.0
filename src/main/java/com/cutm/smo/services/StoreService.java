package com.cutm.smo.services;

import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import com.cutm.smo.util.LoggingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
public class StoreService {
    private final ItemRepository itemRepository;
    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PoItemsRepository poItemsRepository;
    private final GrnRepository grnRepository;
    private final GrnItemsRepository grnItemsRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final BomRepository bomRepository;

    public StoreService(ItemRepository itemRepository, VendorRepository vendorRepository,
            PurchaseOrderRepository purchaseOrderRepository, PoItemsRepository poItemsRepository,
            GrnRepository grnRepository, GrnItemsRepository grnItemsRepository,
            InventoryStockRepository inventoryStockRepository, StockMovementRepository stockMovementRepository,
            BomRepository bomRepository) {
        this.itemRepository = itemRepository;
        this.vendorRepository = vendorRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.poItemsRepository = poItemsRepository;
        this.grnRepository = grnRepository;
        this.grnItemsRepository = grnItemsRepository;
        this.inventoryStockRepository = inventoryStockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.bomRepository = bomRepository;
    }

    public List<Item> getAllItems() { return itemRepository.findAll(); }
    public Item getItemById(Long id) { return itemRepository.findById(id).orElse(null); }
    public Item createItem(Item item) { return itemRepository.save(item); }
    public Item updateItem(Long id, Item item) { item.setItemId(id); return itemRepository.save(item); }
    public void deleteItem(Long id) { itemRepository.deleteById(id); }

    public List<Vendor> getAllVendors() { return vendorRepository.findAll(); }
    public Vendor getVendorById(Long id) { return vendorRepository.findById(id).orElse(null); }
    public Vendor createVendor(Vendor vendor) { return vendorRepository.save(vendor); }
    public Vendor updateVendor(Long id, Vendor vendor) { vendor.setVendorId(id); return vendorRepository.save(vendor); }
    public void deleteVendor(Long id) { vendorRepository.deleteById(id); }

    public List<PurchaseOrder> getAllPurchaseOrders() { return purchaseOrderRepository.findAll(); }
    public PurchaseOrder getPurchaseOrderById(Long id) { return purchaseOrderRepository.findById(id).orElse(null); }
    public PurchaseOrder createPurchaseOrder(PurchaseOrder po) { return purchaseOrderRepository.save(po); }
    public PurchaseOrder updatePurchaseOrder(Long id, PurchaseOrder po) { po.setPoId(id); return purchaseOrderRepository.save(po); }
    public void deletePurchaseOrder(Long id) { purchaseOrderRepository.deleteById(id); }

    public List<PoItems> getAllPoItems() { return poItemsRepository.findAll(); }
    public PoItems getPoItemsById(Long id) { return poItemsRepository.findById(id).orElse(null); }
    public PoItems createPoItems(PoItems items) { return poItemsRepository.save(items); }
    public PoItems updatePoItems(Long id, PoItems items) { items.setPoItemId(id); return poItemsRepository.save(items); }
    public void deletePoItems(Long id) { poItemsRepository.deleteById(id); }

    public List<Grn> getAllGrns() { return grnRepository.findAll(); }
    public Grn getGrnById(Long id) { return grnRepository.findById(id).orElse(null); }
    public Grn createGrn(Grn grn) { return grnRepository.save(grn); }
    public Grn updateGrn(Long id, Grn grn) { grn.setGrnId(id); return grnRepository.save(grn); }
    public void deleteGrn(Long id) { grnRepository.deleteById(id); }

    public List<GrnItems> getAllGrnItems() { return grnItemsRepository.findAll(); }
    public GrnItems getGrnItemsById(Long id) { return grnItemsRepository.findById(id).orElse(null); }
    public GrnItems createGrnItems(GrnItems items) { return grnItemsRepository.save(items); }
    public GrnItems updateGrnItems(Long id, GrnItems items) { items.setGrnItemId(id); return grnItemsRepository.save(items); }
    public void deleteGrnItems(Long id) { grnItemsRepository.deleteById(id); }

    public List<InventoryStock> getAllInventoryStocks() { return inventoryStockRepository.findAll(); }
    public InventoryStock getInventoryStockById(Long id) { return inventoryStockRepository.findById(id).orElse(null); }
    public InventoryStock createInventoryStock(InventoryStock stock) { return inventoryStockRepository.save(stock); }
    public InventoryStock updateInventoryStock(Long id, InventoryStock stock) { stock.setStockId(id); return inventoryStockRepository.save(stock); }
    public void deleteInventoryStock(Long id) { inventoryStockRepository.deleteById(id); }

    public List<StockMovement> getAllStockMovements() { return stockMovementRepository.findAll(); }
    public StockMovement createStockMovement(StockMovement movement) { return stockMovementRepository.save(movement); }

    public List<Bom> getAllBoms() { return bomRepository.findAll(); }
    public Bom getBomById(Long id) { return bomRepository.findById(id).orElse(null); }
    public List<Bom> getBomsByStyleVariantId(Long styleVariantId) { return bomRepository.findAll().stream().filter(b -> b.getStyleVariantId() != null && b.getStyleVariantId().equals(styleVariantId)).toList(); }
    public Bom createBom(Bom bom) { return bomRepository.save(bom); }
    public Bom updateBom(Long id, Bom bom) { bom.setBomId(id); return bomRepository.save(bom); }
    public void deleteBom(Long id) { bomRepository.deleteById(id); }
}