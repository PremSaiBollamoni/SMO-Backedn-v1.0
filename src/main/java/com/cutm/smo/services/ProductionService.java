package com.cutm.smo.services;

import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import com.cutm.smo.util.LoggingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
public class ProductionService {
    private final ProductRepository productRepository;
    private final RoutingRepository routingRepository;
    private final OperationRepository operationRepository;
    private final RoutingStepRepository routingStepRepository;
    private final BundleRepository bundleRepository;
    private final BinRepository binRepository;
    private final GarmentRepository garmentRepository;
    private final WipTrackingRepository wipTrackingRepository;

    public ProductionService(ProductRepository productRepository, RoutingRepository routingRepository, OperationRepository operationRepository,
            RoutingStepRepository routingStepRepository, BundleRepository bundleRepository,
            BinRepository binRepository, GarmentRepository garmentRepository,
            WipTrackingRepository wipTrackingRepository) {
        this.productRepository = productRepository;
        this.routingRepository = routingRepository;
        this.operationRepository = operationRepository;
        this.routingStepRepository = routingStepRepository;
        this.bundleRepository = bundleRepository;
        this.binRepository = binRepository;
        this.garmentRepository = garmentRepository;
        this.wipTrackingRepository = wipTrackingRepository;
    }

    public List<Product> getAllProducts() { return productRepository.findAll(); }
    public Product getProductById(Long id) { return productRepository.findById(id).orElse(null); }
    public Product createProduct(Product product) { return productRepository.save(product); }
    public Product updateProduct(Long id, Product product) { product.setProductId(id); return productRepository.save(product); }
    public void deleteProduct(Long id) { productRepository.deleteById(id); }

    public List<Routing> getAllRoutings() { return routingRepository.findAll(); }
    public Routing getRoutingById(Long id) { return routingRepository.findById(id).orElse(null); }
    public Routing createRouting(Routing routing) { return routingRepository.save(routing); }
    public Routing updateRouting(Long id, Routing routing) { routing.setRoutingId(id); return routingRepository.save(routing); }
    public void deleteRouting(Long id) { routingRepository.deleteById(id); }

    public List<Operation> getAllOperations() { return operationRepository.findAll(); }
    public Operation getOperationById(Long id) { return operationRepository.findById(id).orElse(null); }
    public Operation createOperation(Operation operation) { return operationRepository.save(operation); }
    public Operation updateOperation(Long id, Operation operation) { operation.setOperationId(id); return operationRepository.save(operation); }
    public void deleteOperation(Long id) { operationRepository.deleteById(id); }

    public List<RoutingStep> getAllRoutingSteps() { return routingStepRepository.findAll(); }
    public List<RoutingStep> getRoutingStepsByRoutingId(Long routingId) {
        return routingStepRepository.findAll().stream()
                .filter(rs -> rs.getRoutingId() != null && rs.getRoutingId().equals(routingId))
                .toList();
    }
    public RoutingStep createRoutingStep(RoutingStep step) { return routingStepRepository.save(step); }
    public RoutingStep updateRoutingStep(Long id, RoutingStep step) { step.setRoutingStepId(id); return routingStepRepository.save(step); }
    public void deleteRoutingStep(Long id) { routingStepRepository.deleteById(id); }

    public List<Bundle> getAllBundles() { return bundleRepository.findAll(); }
    public Bundle getBundleById(Long id) { return bundleRepository.findById(id).orElse(null); }
    public Bundle createBundle(Bundle bundle) { return bundleRepository.save(bundle); }
    public Bundle updateBundle(Long id, Bundle bundle) { bundle.setBundleId(id); return bundleRepository.save(bundle); }
    public void deleteBundle(Long id) { bundleRepository.deleteById(id); }

    public List<Bin> getAllBins() { return binRepository.findAll(); }
    public Bin getBinById(Long id) { return binRepository.findById(id).orElse(null); }
    public Bin createBin(Bin bin) { return binRepository.save(bin); }
    public Bin updateBin(Long id, Bin bin) { bin.setBinId(id); return binRepository.save(bin); }
    public void deleteBin(Long id) { binRepository.deleteById(id); }

    public List<Garment> getAllGarments() { return garmentRepository.findAll(); }
    public Garment getGarmentById(Long id) { return garmentRepository.findById(id).orElse(null); }
    public Garment createGarment(Garment garment) { return garmentRepository.save(garment); }
    public Garment updateGarment(Long id, Garment garment) { garment.setGarmentId(id); return garmentRepository.save(garment); }
    public void deleteGarment(Long id) { garmentRepository.deleteById(id); }

    public List<WipTracking> getAllWipTrackings() { return wipTrackingRepository.findAll(); }
    public WipTracking getWipTrackingById(Long id) { return wipTrackingRepository.findById(id).orElse(null); }
    public WipTracking createWipTracking(WipTracking wip) { return wipTrackingRepository.save(wip); }
    public WipTracking updateWipTracking(Long id, WipTracking wip) { wip.setWipId(id); return wipTrackingRepository.save(wip); }
    public void deleteWipTracking(Long id) { wipTrackingRepository.deleteById(id); }
}