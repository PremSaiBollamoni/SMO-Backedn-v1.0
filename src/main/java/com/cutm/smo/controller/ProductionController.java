package com.cutm.smo.controller;

import com.cutm.smo.models.*;
import com.cutm.smo.services.*;
import com.cutm.smo.util.LoggingUtil;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/production")
@CrossOrigin(origins = "*")
public class ProductionController {
    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) { this.productionService = productionService; }
    
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

    @GetMapping("/products")
    public List<Product> getAllProducts() { 
        return executeWithLogging("GET ALL PRODUCTS", "Product", null, () -> productionService.getAllProducts()); 
    }
    @GetMapping("/products/{id}")
    public Product getProductById(@PathVariable Long id) { 
        return executeWithLogging("GET PRODUCT BY ID", "Product", id, () -> productionService.getProductById(id)); 
    }
    @PostMapping("/products")
    public Product createProduct(@RequestBody Product product) { 
        return executeWithLogging("CREATE PRODUCT", "Product", null, () -> productionService.createProduct(product)); 
    }
    @PutMapping("/products/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product product) { 
        return executeWithLogging("UPDATE PRODUCT", "Product", id, () -> productionService.updateProduct(id, product)); 
    }
    @DeleteMapping("/products/{id}")
    public void deleteProduct(@PathVariable Long id) { 
        executeWithLogging("DELETE PRODUCT", "Product", id, () -> { productionService.deleteProduct(id); return null; }); 
    }

    @GetMapping("/routings")
    public List<Routing> getAllRoutings() { 
        return executeWithLogging("GET ALL ROUTINGS", "Routing", null, () -> productionService.getAllRoutings()); 
    }
    @GetMapping("/routings/{id}")
    public Routing getRoutingById(@PathVariable Long id) { 
        return executeWithLogging("GET ROUTING BY ID", "Routing", id, () -> productionService.getRoutingById(id)); 
    }
    @PostMapping("/routings")
    public Routing createRouting(@RequestBody Routing routing) { 
        return executeWithLogging("CREATE ROUTING", "Routing", null, () -> productionService.createRouting(routing)); 
    }
    @PutMapping("/routings/{id}")
    public Routing updateRouting(@PathVariable Long id, @RequestBody Routing routing) { 
        return executeWithLogging("UPDATE ROUTING", "Routing", id, () -> productionService.updateRouting(id, routing)); 
    }
    @DeleteMapping("/routings/{id}")
    public void deleteRouting(@PathVariable Long id) { 
        executeWithLogging("DELETE ROUTING", "Routing", id, () -> { productionService.deleteRouting(id); return null; }); 
    }

    @GetMapping("/operations")
    public List<Operation> getAllOperations() { 
        return executeWithLogging("GET ALL OPERATIONS", "Operation", null, () -> productionService.getAllOperations()); 
    }
    @GetMapping("/operations/{id}")
    public Operation getOperationById(@PathVariable Long id) { 
        return executeWithLogging("GET OPERATION BY ID", "Operation", id, () -> productionService.getOperationById(id)); 
    }
    @PostMapping("/operations")
    public Operation createOperation(@RequestBody Operation operation) { 
        return executeWithLogging("CREATE OPERATION", "Operation", null, () -> productionService.createOperation(operation)); 
    }
    @PutMapping("/operations/{id}")
    public Operation updateOperation(@PathVariable Long id, @RequestBody Operation operation) { 
        return executeWithLogging("UPDATE OPERATION", "Operation", id, () -> productionService.updateOperation(id, operation)); 
    }
    @DeleteMapping("/operations/{id}")
    public void deleteOperation(@PathVariable Long id) { 
        executeWithLogging("DELETE OPERATION", "Operation", id, () -> { productionService.deleteOperation(id); return null; }); 
    }

    @GetMapping("/routingsteps")
    public List<RoutingStep> getAllRoutingSteps() { 
        return executeWithLogging("GET ALL ROUTING STEPS", "RoutingStep", null, () -> productionService.getAllRoutingSteps()); 
    }
    @GetMapping("/routingsteps/routing/{routingId}")
    public List<RoutingStep> getRoutingStepsByRoutingId(@PathVariable Long routingId) { 
        return executeWithLogging("GET ROUTING STEPS BY ROUTING ID", "RoutingStep", routingId, () -> productionService.getRoutingStepsByRoutingId(routingId)); 
    }
    @PostMapping("/routingsteps")
    public RoutingStep createRoutingStep(@RequestBody RoutingStep step) { 
        return executeWithLogging("CREATE ROUTING STEP", "RoutingStep", null, () -> productionService.createRoutingStep(step)); 
    }
    @PutMapping("/routingsteps/{id}")
    public RoutingStep updateRoutingStep(@PathVariable Long id, @RequestBody RoutingStep step) { 
        return executeWithLogging("UPDATE ROUTING STEP", "RoutingStep", id, () -> productionService.updateRoutingStep(id, step)); 
    }
    @DeleteMapping("/routingsteps/{id}")
    public void deleteRoutingStep(@PathVariable Long id) { 
        executeWithLogging("DELETE ROUTING STEP", "RoutingStep", id, () -> { productionService.deleteRoutingStep(id); return null; }); 
    }

    @GetMapping("/bundles")
    public List<Bundle> getAllBundles() { 
        return executeWithLogging("GET ALL BUNDLES", "Bundle", null, () -> productionService.getAllBundles()); 
    }
    @GetMapping("/bundles/{id}")
    public Bundle getBundleById(@PathVariable Long id) { 
        return executeWithLogging("GET BUNDLE BY ID", "Bundle", id, () -> productionService.getBundleById(id)); 
    }
    @PostMapping("/bundles")
    public Bundle createBundle(@RequestBody Bundle bundle) { 
        return executeWithLogging("CREATE BUNDLE", "Bundle", null, () -> productionService.createBundle(bundle)); 
    }
    @PutMapping("/bundles/{id}")
    public Bundle updateBundle(@PathVariable Long id, @RequestBody Bundle bundle) { 
        return executeWithLogging("UPDATE BUNDLE", "Bundle", id, () -> productionService.updateBundle(id, bundle)); 
    }
    @DeleteMapping("/bundles/{id}")
    public void deleteBundle(@PathVariable Long id) { 
        executeWithLogging("DELETE BUNDLE", "Bundle", id, () -> { productionService.deleteBundle(id); return null; }); 
    }

    @GetMapping("/bins")
    public List<Bin> getAllBins() { 
        return executeWithLogging("GET ALL BINS", "Bin", null, () -> productionService.getAllBins()); 
    }
    @GetMapping("/bins/{id}")
    public Bin getBinById(@PathVariable Long id) { 
        return executeWithLogging("GET BIN BY ID", "Bin", id, () -> productionService.getBinById(id)); 
    }
    @PostMapping("/bins")
    public Bin createBin(@RequestBody Bin bin) { 
        return executeWithLogging("CREATE BIN", "Bin", null, () -> productionService.createBin(bin)); 
    }
    @PutMapping("/bins/{id}")
    public Bin updateBin(@PathVariable Long id, @RequestBody Bin bin) { 
        return executeWithLogging("UPDATE BIN", "Bin", id, () -> productionService.updateBin(id, bin)); 
    }
    @DeleteMapping("/bins/{id}")
    public void deleteBin(@PathVariable Long id) { 
        executeWithLogging("DELETE BIN", "Bin", id, () -> { productionService.deleteBin(id); return null; }); 
    }

    @GetMapping("/garments")
    public List<Garment> getAllGarments() { 
        return executeWithLogging("GET ALL GARMENTS", "Garment", null, () -> productionService.getAllGarments()); 
    }
    @GetMapping("/garments/{id}")
    public Garment getGarmentById(@PathVariable Long id) { 
        return executeWithLogging("GET GARMENT BY ID", "Garment", id, () -> productionService.getGarmentById(id)); 
    }
    @PostMapping("/garments")
    public Garment createGarment(@RequestBody Garment garment) { 
        return executeWithLogging("CREATE GARMENT", "Garment", null, () -> productionService.createGarment(garment)); 
    }
    @PutMapping("/garments/{id}")
    public Garment updateGarment(@PathVariable Long id, @RequestBody Garment garment) { 
        return executeWithLogging("UPDATE GARMENT", "Garment", id, () -> productionService.updateGarment(id, garment)); 
    }
    @DeleteMapping("/garments/{id}")
    public void deleteGarment(@PathVariable Long id) { 
        executeWithLogging("DELETE GARMENT", "Garment", id, () -> { productionService.deleteGarment(id); return null; }); 
    }

    @GetMapping("/wiptracking")
    public List<WipTracking> getAllWipTrackings() { 
        return executeWithLogging("GET ALL WIP TRACKINGS", "WipTracking", null, () -> productionService.getAllWipTrackings()); 
    }
    @GetMapping("/wiptracking/{id}")
    public WipTracking getWipTrackingById(@PathVariable Long id) { 
        return executeWithLogging("GET WIP TRACKING BY ID", "WipTracking", id, () -> productionService.getWipTrackingById(id)); 
    }
    @PostMapping("/wiptracking")
    public WipTracking createWipTracking(@RequestBody WipTracking wip) { 
        return executeWithLogging("CREATE WIP TRACKING", "WipTracking", null, () -> productionService.createWipTracking(wip)); 
    }
    @PutMapping("/wiptracking/{id}")
    public WipTracking updateWipTracking(@PathVariable Long id, @RequestBody WipTracking wip) { 
        return executeWithLogging("UPDATE WIP TRACKING", "WipTracking", id, () -> productionService.updateWipTracking(id, wip)); 
    }
    @DeleteMapping("/wiptracking/{id}")
    public void deleteWipTracking(@PathVariable Long id) { 
        executeWithLogging("DELETE WIP TRACKING", "WipTracking", id, () -> { productionService.deleteWipTracking(id); return null; }); 
    }
}