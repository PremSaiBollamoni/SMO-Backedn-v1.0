package com.cutm.smo.services;

import com.cutm.smo.dto.QrAssignmentRequest;
import com.cutm.smo.dto.TrackingRequest;
import com.cutm.smo.dto.MergingRequest;
import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SupervisorService {

    @Autowired
    private RoutingRepository routingRepository;

    @Autowired
    private StyleRepository styleRepository;

    @Autowired
    private StyleVariantRepository styleVariantRepository;

    @Autowired
    private ButtonsRepository buttonsRepository;

    @Autowired
    private GarmentRepository garmentRepository;
    
    @Autowired
    private WipTrackingRepository wipTrackingRepository;
    
    @Autowired
    private BinRepository binRepository;

    @Autowired
    private EnhancedTrackingService enhancedTrackingService;

    @Autowired
    private EnhancedMergingService enhancedMergingService;

    @Autowired
    private EnhancedQrAssignmentService enhancedQrAssignmentService;

    @Autowired
    private com.cutm.smo.repositories.RoutingStepRepository routingStepRepository;

    @Autowired
    private com.cutm.smo.repositories.OperationRepository operationRepository;

    @Autowired
    private TempActiveAssignmentRepository tempActiveAssignmentRepository; // NEW: For team assignment lookup

    @Autowired
    private com.cutm.smo.repositories.EmployeeRepository employeeRepository; // NEW: For fetching employee names

    /**
     * Get all approved process plan numbers (routing IDs)
     */
    public List<String> getProcessPlans() {
        List<Routing> routings = routingRepository.findAll();
        return routings.stream()
                .filter(r -> "APPROVED".equalsIgnoreCase(r.getApprovalStatus()))
                .map(r -> String.valueOf(r.getRoutingId()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all style numbers
     */
    public List<String> getStyles() {
        List<Style> styles = styleRepository.findAll();
        return styles.stream()
                .filter(s -> s.getStyleNo() != null && !s.getStyleNo().trim().isEmpty())
                .map(Style::getStyleNo)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all sizes from style_variant table
     */
    public List<String> getSizes() {
        List<StyleVariant> variants = styleVariantRepository.findAll();
        return variants.stream()
                .filter(v -> v.getSize() != null && !v.getSize().trim().isEmpty())
                .map(StyleVariant::getSize)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all GTG numbers from style_variant table
     */
    public List<String> getGtgNumbers() {
        List<StyleVariant> variants = styleVariantRepository.findAll();
        return variants.stream()
                .filter(v -> v.getGtgId() != null && !v.getGtgId().trim().isEmpty())
                .map(StyleVariant::getGtgId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all button numbers from buttons table
     */
    public List<String> getBtnNumbers() {
        List<Buttons> buttons = buttonsRepository.findAll();
        return buttons.stream()
                .filter(b -> b.getButtonCode() != null && !b.getButtonCode().trim().isEmpty())
                .map(Buttons::getButtonCode)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all labels from style table (main_label and branding_label)
     */
    public List<String> getLabels() {
        List<Style> styles = styleRepository.findAll();
        Set<String> labels = new HashSet<>();
        
        for (Style style : styles) {
            if (style.getMainLabel() != null && !style.getMainLabel().trim().isEmpty()) {
                labels.add(style.getMainLabel());
            }
            if (style.getBrandingLabel() != null && !style.getBrandingLabel().trim().isEmpty()) {
                labels.add(style.getBrandingLabel());
            }
        }
        
        return labels.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get operations for a specific routing/process plan
     */
    public List<Map<String, Object>> getOperationsForRouting(Long routingId) {
        List<com.cutm.smo.models.RoutingStep> routingSteps = routingStepRepository.findByRoutingId(routingId);
        
        return routingSteps.stream()
                .map(step -> {
                    Map<String, Object> opData = new HashMap<>();
                    opData.put("operationId", step.getOperationId());
                    
                    // Fetch operation details
                    operationRepository.findById(step.getOperationId()).ifPresent(op -> {
                        opData.put("name", op.getName());
                        opData.put("sequence", op.getSequence());
                        opData.put("stageGroup", step.getStageGroup());
                    });
                    
                    return opData;
                })
                .filter(op -> op.containsKey("name"))
                .sorted((a, b) -> {
                    Integer seqA = (Integer) a.getOrDefault("sequence", 0);
                    Integer seqB = (Integer) b.getOrDefault("sequence", 0);
                    return seqA.compareTo(seqB);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get bin current operation by tray QR code
     * Returns bin info including current_operation_id and operation name
     */
    public Map<String, Object> getBinCurrentOperation(String trayQr) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<Bin> binOpt = binRepository.findByQrCode(trayQr);
        
        if (!binOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "Bin not found for tray QR: " + trayQr);
            return response;
        }
        
        Bin bin = binOpt.get();
        
        response.put("success", true);
        response.put("binId", bin.getBinId());
        response.put("qrCode", bin.getQrCode());
        response.put("currentOperationId", bin.getCurrentOperationId());
        response.put("lastOperationId", bin.getLastOperationId());
        response.put("currentRoutingId", bin.getCurrentRoutingId());
        response.put("status", bin.getStatus());
        response.put("currentStatus", bin.getCurrentStatus());
        
        // Fetch operation name if current operation exists
        if (bin.getCurrentOperationId() != null) {
            operationRepository.findById(bin.getCurrentOperationId()).ifPresent(op -> {
                response.put("currentOperationName", op.getName());
                response.put("currentOperationSequence", op.getSequence());
            });
        }
        
        return response;
    }

    /**
     * Submit QR assignment using enhanced workflow
     * Includes transaction management, status validation, and assignment history
     */
    @Transactional
    public Map<String, Object> submitQrAssignment(QrAssignmentRequest request) {
        return enhancedQrAssignmentService.processEnhancedQrAssignment(request);
    }
    
    /**
     * Find style variant ID based on the provided data
     * Returns null if no matching variant found
     */
    private Long findStyleVariantId(QrAssignmentRequest request) {
        List<StyleVariant> variants = styleVariantRepository.findAll();
        
        for (StyleVariant variant : variants) {
            boolean matches = true;
            
            // Match size if provided
            if (request.getSize() != null && !request.getSize().trim().isEmpty()) {
                if (variant.getSize() == null || !variant.getSize().equalsIgnoreCase(request.getSize())) {
                    matches = false;
                }
            }
            
            // Match GTG number if provided
            if (request.getGtgNumber() != null && !request.getGtgNumber().trim().isEmpty()) {
                if (variant.getGtgId() == null || !variant.getGtgId().equalsIgnoreCase(request.getGtgNumber())) {
                    matches = false;
                }
            }
            
            if (matches) {
                return variant.getStyleVariantId();
            }
        }
        
        return null;
    }
    
    /**
     * Submit tracking data using enhanced two-phase workflow
     * Automatically detects assignment vs completion
     */
    @Transactional
    public Map<String, Object> submitTracking(TrackingRequest request) {
        return enhancedTrackingService.processTracking(request);
    }
    
    /**
     * Submit merging data using enhanced workflow
     * Includes compatibility validation and multi-table updates
     */
    @Transactional
    public Map<String, Object> submitMerging(MergingRequest request) {
        return enhancedMergingService.processEnhancedMerging(request);
    }

    /**
     * Get active workers at a specific operation (NEW - multi-employee tracking)
     * Returns list of employees currently assigned to bins at the specified operation
     */
    public List<Map<String, Object>> getActiveWorkersAtOperation(Long operationId) {
        List<Map<String, Object>> workers = new ArrayList<>();
        
        try {
            System.out.println("[SUPERVISOR] Getting active workers for operation " + operationId);
            
            // === APPROACH 1: Check temp_active_assignments (live assignments) ===
            List<Bin> binsAtOperation = binRepository.findAll().stream()
                .filter(bin -> bin.getCurrentOperationId() != null && bin.getCurrentOperationId().equals(operationId))
                .collect(Collectors.toList());
            
            System.out.println("[SUPERVISOR] Found " + binsAtOperation.size() + " bins at operation " + operationId);
            
            // For each bin, find active assignments from temp_active_assignments
            for (Bin bin : binsAtOperation) {
                String trayQr = bin.getQrCode();
                System.out.println("[SUPERVISOR] Checking bin: " + trayQr);
                
                // Find any active assignment for this tray
                // Include both "assigned" (active work) and "completed" (between bundles - continuous tracking)
                List<TempActiveAssignment> assignments = tempActiveAssignmentRepository.findAll().stream()
                    .filter(a -> a.getTrayQr() != null && a.getTrayQr().equals(trayQr))
                    .filter(a -> "assigned".equalsIgnoreCase(a.getStatus()) || "completed".equalsIgnoreCase(a.getStatus()))
                    .collect(Collectors.toList());
                
                if (!assignments.isEmpty()) {
                    TempActiveAssignment assignment = assignments.get(0);
                    System.out.println("[SUPERVISOR] Found temp assignment for tray " + trayQr + " with empIds: " + assignment.getEmpIds());
                    
                    // Parse employees from this assignment
                    if (assignment.getEmpIds() != null && !assignment.getEmpIds().trim().isEmpty()) {
                        String empIdsJson = assignment.getEmpIds();
                        String[] empIdStrs = empIdsJson.replaceAll("[\\[\\]\\s]", "").split(",");
                        
                        for (String empIdStr : empIdStrs) {
                            try {
                                Long empId = Long.parseLong(empIdStr.trim());
                                String employeeName = fetchEmployeeName(empId);
                                
                                Map<String, Object> worker = new HashMap<>();
                                worker.put("employeeId", empId);
                                worker.put("employeeName", employeeName);
                                worker.put("trayQr", trayQr);
                                worker.put("machineQr", assignment.getMachineQr());
                                worker.put("startTime", assignment.getStartTime());
                                workers.add(worker);
                                System.out.println("[SUPERVISOR] Added worker from temp: " + empId + " (" + employeeName + ")");
                            } catch (NumberFormatException e) {
                                System.out.println("[SUPERVISOR] Failed to parse employee ID: " + empIdStr);
                            }
                        }
                    } else if (assignment.getEmpId() != null) {
                        Long empId = assignment.getEmpId();
                        String employeeName = fetchEmployeeName(empId);
                        
                        Map<String, Object> worker = new HashMap<>();
                        worker.put("employeeId", empId);
                        worker.put("employeeName", employeeName);
                        worker.put("trayQr", trayQr);
                        worker.put("machineQr", assignment.getMachineQr());
                        worker.put("startTime", assignment.getStartTime());
                        workers.add(worker);
                        System.out.println("[SUPERVISOR] Added single worker from temp: " + empId + " (" + employeeName + ")");
                    }
                }
            }
            
            // === APPROACH 2: Check wiptracking for today (continuous tracking) ===
            // If no workers found in temp_active_assignments, check wiptracking with today's date
            if (workers.isEmpty()) {
                System.out.println("[SUPERVISOR] No workers in temp_active_assignments, checking wiptracking...");
                
                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.LocalDateTime startOfDay = today.atStartOfDay();
                java.time.LocalDateTime endOfDay = today.atTime(23, 59, 59);
                
                List<WipTracking> todayWipRecords = wipTrackingRepository.findAll().stream()
                    .filter(w -> w.getOperationId() != null && w.getOperationId().equals(operationId))
                    .filter(w -> w.getStartTime() != null)
                    .filter(w -> w.getStartTime().isAfter(startOfDay) && w.getStartTime().isBefore(endOfDay))
                    .filter(w -> w.getOperatorId() != null)
                    // Include both IN_PROGRESS and COMPLETED records (continuous tracking)
                    .filter(w -> "IN_PROGRESS".equalsIgnoreCase(w.getStatus()) || "COMPLETED".equalsIgnoreCase(w.getStatus()) || "Completed".equalsIgnoreCase(w.getStatus()))
                    .collect(Collectors.toList());
                
                System.out.println("[SUPERVISOR] Found " + todayWipRecords.size() + " WIP records for operation " + operationId + " today");
                
                // Get distinct operators from today's WIP records
                Set<Long> distinctOperators = todayWipRecords.stream()
                    .map(WipTracking::getOperatorId)
                    .filter(opId -> opId != null)
                    .collect(Collectors.toSet());
                
                System.out.println("[SUPERVISOR] Found " + distinctOperators.size() + " distinct operators today at operation " + operationId);
                
                for (Long empId : distinctOperators) {
                    String employeeName = fetchEmployeeName(empId);
                    
                    // Get latest record for this operator at this operation
                    WipTracking latestRecord = todayWipRecords.stream()
                        .filter(w -> w.getOperatorId().equals(empId))
                        .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                        .findFirst()
                        .orElse(null);
                    
                    Map<String, Object> worker = new HashMap<>();
                    worker.put("employeeId", empId);
                    worker.put("employeeName", employeeName);
                    worker.put("operationId", operationId);
                    if (latestRecord != null) {
                        worker.put("startTime", latestRecord.getStartTime());
                        worker.put("binId", latestRecord.getBinId());
                    }
                    workers.add(worker);
                    System.out.println("[SUPERVISOR] Added worker from wiptracking: " + empId + " (" + employeeName + ")");
                }
            }
            
            System.out.println("[SUPERVISOR] Returning " + workers.size() + " active workers at operation " + operationId);
        } catch (Exception e) {
            System.out.println("[SUPERVISOR] Error fetching active workers: " + e.getMessage());
            e.printStackTrace();
        }
        
        return workers;
    }

    /**
     * Fetch employee name from employee table by ID
     */
    private String fetchEmployeeName(Long empId) {
        try {
            java.util.Optional<com.cutm.smo.models.EmployeeInfo> empOpt = employeeRepository.findById(empId);
            if (empOpt.isPresent()) {
                return empOpt.get().getEmpName();
            }
        } catch (Exception e) {
            System.out.println("[SUPERVISOR] Error fetching employee name for ID " + empId + ": " + e.getMessage());
        }
        return "Employee " + empId; // Fallback
    }
}
