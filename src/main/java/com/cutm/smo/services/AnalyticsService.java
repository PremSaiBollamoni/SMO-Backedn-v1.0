package com.cutm.smo.services;

import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import com.cutm.smo.repository.HourlyTargetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Service — provides data for Operations, Employee, and Machine dashboards.
 * READ-ONLY: does not modify any existing data.
 */
@Service
public class AnalyticsService {

    @Autowired
    private WipTrackingRepository wipTrackingRepository;

    @Autowired
    private EmployeeInfoRepository employeeInfoRepository;

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private HourlyTargetRepository hourlyTargetRepository;

    @Autowired
    private RoutingRepository routingRepository;

    // ─── Operations Analytics ───────────────────────────────────────────────

    /**
     * Get all approved routings for the analytics routing picker.
     */
    public List<Map<String, Object>> getApprovedRoutings() {
        return routingRepository.findAll().stream()
            .filter(r -> "APPROVED".equalsIgnoreCase(r.getApprovalStatus()))
            .map(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("routingId", r.getRoutingId());
                m.put("productId", r.getProductId());
                m.put("version", r.getVersion());
                m.put("status", r.getStatus());
                m.put("approvalStatus", r.getApprovalStatus());
                return m;
            })
            .collect(Collectors.toList());
    }

    /**
     * Get all operations for a routing with employee count and actual vs target for a date.
     */
    public List<Map<String, Object>> getOperationsForRouting(Long routingId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        // Get all wip records for this date
        List<WipTracking> wipRecords = wipTrackingRepository.findAll().stream()
            .filter(w -> w.getStartTime() != null && !w.getStartTime().isBefore(start) && !w.getStartTime().isAfter(end))
            .collect(Collectors.toList());

        // Group by operation
        Map<Long, List<WipTracking>> byOperation = wipRecords.stream()
            .filter(w -> w.getOperationId() != null)
            .collect(Collectors.groupingBy(WipTracking::getOperationId));

        // Get distinct operations from wip records + routing steps
        Set<Long> operationIds = new HashSet<>(byOperation.keySet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long opId : operationIds) {
            List<WipTracking> opRecords = byOperation.getOrDefault(opId, Collections.emptyList());
            int totalQty = opRecords.stream().mapToInt(w -> w.getQty() != null ? w.getQty() : 0).sum();
            long activeWorkers = opRecords.stream()
                .filter(w -> "IN_PROGRESS".equals(w.getStatus()))
                .map(WipTracking::getOperatorId).filter(Objects::nonNull).distinct().count();

            Optional<HourlyTarget> target = hourlyTargetRepository.findByOperationId(opId);
            int targetPerHour = target.map(HourlyTarget::getTargetPerHour).orElse(30);
            String opName = target.map(HourlyTarget::getOperationName).orElse("Operation " + opId);

            Map<String, Object> m = new HashMap<>();
            m.put("operationId", opId);
            m.put("operationName", opName);
            m.put("totalQty", totalQty);
            m.put("targetPerHour", targetPerHour);
            m.put("activeWorkers", activeWorkers);
            result.add(m);
        }

        result.sort(Comparator.comparing(m -> (String) m.get("operationName")));
        return result;
    }

    /**
     * Get employees working at a specific operation on a date with their stats.
     */
    public List<Map<String, Object>> getEmployeesAtOperation(Long operationId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<WipTracking> records = wipTrackingRepository.findAll().stream()
            .filter(w -> operationId.equals(w.getOperationId()))
            .filter(w -> w.getStartTime() != null && !w.getStartTime().isBefore(start) && !w.getStartTime().isAfter(end))
            .collect(Collectors.toList());

        Map<Long, List<WipTracking>> byEmployee = records.stream()
            .filter(w -> w.getOperatorId() != null)
            .collect(Collectors.groupingBy(WipTracking::getOperatorId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<WipTracking>> entry : byEmployee.entrySet()) {
            Long empId = entry.getKey();
            List<WipTracking> empRecords = entry.getValue();

            String empName = employeeInfoRepository.findById(empId)
                .map(EmployeeInfo::getEmpName).orElse("Employee " + empId);

            int totalQty = empRecords.stream().mapToInt(w -> w.getQty() != null ? w.getQty() : 0).sum();
            long completedCount = empRecords.stream().filter(w -> "COMPLETED".equals(w.getStatus())).count();

            Map<String, Object> m = new HashMap<>();
            m.put("employeeId", empId);
            m.put("employeeName", empName);
            m.put("totalQty", totalQty);
            m.put("completedCount", completedCount);
            result.add(m);
        }

        result.sort(Comparator.comparing(m -> (String) m.get("employeeName")));
        return result;
    }

    // ─── Employee Analytics ──────────────────────────────────────────────────

    /**
     * Get all employees with summary stats for a date.
     */
    public List<Map<String, Object>> getAllEmployeesWithStats(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<WipTracking> records = wipTrackingRepository.findAll().stream()
            .filter(w -> w.getStartTime() != null && !w.getStartTime().isBefore(start) && !w.getStartTime().isAfter(end))
            .filter(w -> w.getOperatorId() != null)
            .collect(Collectors.toList());

        Map<Long, List<WipTracking>> byEmployee = records.stream()
            .collect(Collectors.groupingBy(WipTracking::getOperatorId));

        // Also include all employees even if no records
        List<EmployeeInfo> allEmployees = employeeInfoRepository.findAll();

        List<Map<String, Object>> result = new ArrayList<>();
        for (EmployeeInfo emp : allEmployees) {
            List<WipTracking> empRecords = byEmployee.getOrDefault(emp.getEmpId(), Collections.emptyList());
            int totalQty = empRecords.stream().mapToInt(w -> w.getQty() != null ? w.getQty() : 0).sum();
            long completedCount = empRecords.stream().filter(w -> "COMPLETED".equals(w.getStatus())).count();

            Map<String, Object> m = new HashMap<>();
            m.put("employeeId", emp.getEmpId());
            m.put("employeeName", emp.getEmpName());
            m.put("role", emp.getRole() != null ? emp.getRole().getRoleName() : "");
            m.put("totalQty", totalQty);
            m.put("completedCount", completedCount);
            m.put("hasActivity", !empRecords.isEmpty());
            result.add(m);
        }

        result.sort((a, b) -> Integer.compare((int) b.get("totalQty"), (int) a.get("totalQty")));
        return result;
    }

    /**
     * Get detailed stats for a specific employee on a date.
     * Returns: hourly breakdown, per-operation breakdown, KPI summary.
     */
    public Map<String, Object> getEmployeeStats(Long employeeId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<WipTracking> records = wipTrackingRepository.findAll().stream()
            .filter(w -> employeeId.equals(w.getOperatorId()))
            .filter(w -> w.getStartTime() != null && !w.getStartTime().isBefore(start) && !w.getStartTime().isAfter(end))
            .collect(Collectors.toList());

        String empName = employeeInfoRepository.findById(employeeId)
            .map(EmployeeInfo::getEmpName).orElse("Employee " + employeeId);

        // Hourly breakdown (0-23)
        Map<Integer, Integer> hourlyQty = new TreeMap<>();
        Map<Integer, Integer> hourlyTarget = new TreeMap<>();
        for (WipTracking w : records) {
            int hour = w.getStartTime().getHour();
            int qty = w.getQty() != null ? w.getQty() : 0;
            hourlyQty.merge(hour, qty, Integer::sum);

            // Get target for this operation
            if (w.getOperationId() != null) {
                int target = hourlyTargetRepository.findByOperationId(w.getOperationId())
                    .map(HourlyTarget::getTargetPerHour).orElse(30);
                hourlyTarget.put(hour, target);
            }
        }

        List<Map<String, Object>> hourlyData = new ArrayList<>();
        for (int h = 6; h <= 20; h++) {
            Map<String, Object> hm = new HashMap<>();
            hm.put("hour", h);
            hm.put("label", String.format("%02d:00", h));
            hm.put("actual", hourlyQty.getOrDefault(h, 0));
            hm.put("target", hourlyTarget.getOrDefault(h, 30));
            hourlyData.add(hm);
        }

        // Per-operation breakdown
        Map<Long, List<WipTracking>> byOperation = records.stream()
            .filter(w -> w.getOperationId() != null)
            .collect(Collectors.groupingBy(WipTracking::getOperationId));

        List<Map<String, Object>> operationData = new ArrayList<>();
        for (Map.Entry<Long, List<WipTracking>> entry : byOperation.entrySet()) {
            Long opId = entry.getKey();
            List<WipTracking> opRecords = entry.getValue();
            int qty = opRecords.stream().mapToInt(w -> w.getQty() != null ? w.getQty() : 0).sum();
            String opName = hourlyTargetRepository.findByOperationId(opId)
                .map(HourlyTarget::getOperationName).orElse("Op " + opId);
            int target = hourlyTargetRepository.findByOperationId(opId)
                .map(HourlyTarget::getTargetPerHour).orElse(30);

            Map<String, Object> om = new HashMap<>();
            om.put("operationId", opId);
            om.put("operationName", opName);
            om.put("actual", qty);
            om.put("target", target);
            om.put("aboveTarget", qty >= target);
            operationData.add(om);
        }

        // KPI summary
        int totalQty = records.stream().mapToInt(w -> w.getQty() != null ? w.getQty() : 0).sum();
        long completedCount = records.stream().filter(w -> "COMPLETED".equals(w.getStatus())).count();
        double avgDurationMinutes = records.stream()
            .filter(w -> w.getStartTime() != null && w.getEndTime() != null)
            .mapToLong(w -> java.time.Duration.between(w.getStartTime(), w.getEndTime()).toMinutes())
            .average().orElse(0);

        Map<String, Object> result = new HashMap<>();
        result.put("employeeId", employeeId);
        result.put("employeeName", empName);
        result.put("date", date.toString());
        result.put("totalQty", totalQty);
        result.put("completedCount", completedCount);
        result.put("avgDurationMinutes", Math.round(avgDurationMinutes));
        result.put("hourlyData", hourlyData);
        result.put("operationData", operationData);
        return result;
    }

    // ─── Machine Analytics ───────────────────────────────────────────────────

    /**
     * Get all machines with utilisation summary for a date.
     */
    public List<Map<String, Object>> getAllMachinesWithStats(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<WipTracking> records = wipTrackingRepository.findAll().stream()
            .filter(w -> w.getStartTime() != null && !w.getStartTime().isBefore(start) && !w.getStartTime().isAfter(end))
            .filter(w -> w.getMachineId() != null)
            .collect(Collectors.toList());

        Map<Long, List<WipTracking>> byMachine = records.stream()
            .collect(Collectors.groupingBy(WipTracking::getMachineId));

        List<com.cutm.smo.models.Machine> allMachines = machineRepository.findAll();

        List<Map<String, Object>> result = new ArrayList<>();
        for (com.cutm.smo.models.Machine machine : allMachines) {
            List<WipTracking> machineRecords = byMachine.getOrDefault(machine.getMachineId(), Collections.emptyList());

            long activeMinutes = machineRecords.stream()
                .filter(w -> w.getStartTime() != null && w.getEndTime() != null)
                .mapToLong(w -> java.time.Duration.between(w.getStartTime(), w.getEndTime()).toMinutes())
                .sum();

            Map<String, Object> m = new HashMap<>();
            m.put("machineId", machine.getMachineId());
            m.put("machineName", machine.getName());
            m.put("machineType", machine.getType());
            m.put("status", machine.getStatus());
            m.put("activeMinutes", activeMinutes);
            m.put("activeHours", Math.round(activeMinutes / 60.0 * 10) / 10.0);
            m.put("recordCount", machineRecords.size());
            result.add(m);
        }

        result.sort((a, b) -> Long.compare((long) b.get("activeMinutes"), (long) a.get("activeMinutes")));
        return result;
    }

    /**
     * Get detailed utilisation stats for a specific machine on a date.
     */
    public Map<String, Object> getMachineStats(String machineId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<WipTracking> records = wipTrackingRepository.findAll().stream()
            .filter(w -> machineId.equals(w.getMachineId()))
            .filter(w -> w.getStartTime() != null && !w.getStartTime().isBefore(start) && !w.getStartTime().isAfter(end))
            .collect(Collectors.toList());

        com.cutm.smo.models.Machine machine = machineRepository.findById(machineId).orElse(null);
        String machineName = machine != null ? machine.getName() : "Machine " + machineId;

        // Active minutes
        long activeMinutes = records.stream()
            .filter(w -> w.getStartTime() != null && w.getEndTime() != null)
            .mapToLong(w -> java.time.Duration.between(w.getStartTime(), w.getEndTime()).toMinutes())
            .sum();

        // Working day = 8 hours = 480 minutes
        long workingDayMinutes = 480;
        long idleMinutes = Math.max(0, workingDayMinutes - activeMinutes);

        // Per-operation breakdown
        Map<Long, List<WipTracking>> byOperation = records.stream()
            .filter(w -> w.getOperationId() != null)
            .collect(Collectors.groupingBy(WipTracking::getOperationId));

        List<Map<String, Object>> operationData = new ArrayList<>();
        for (Map.Entry<Long, List<WipTracking>> entry : byOperation.entrySet()) {
            Long opId = entry.getKey();
            List<WipTracking> opRecords = entry.getValue();
            long opMinutes = opRecords.stream()
                .filter(w -> w.getStartTime() != null && w.getEndTime() != null)
                .mapToLong(w -> java.time.Duration.between(w.getStartTime(), w.getEndTime()).toMinutes())
                .sum();
            String opName = hourlyTargetRepository.findByOperationId(opId)
                .map(HourlyTarget::getOperationName).orElse("Op " + opId);

            Map<String, Object> om = new HashMap<>();
            om.put("operationId", opId);
            om.put("operationName", opName);
            om.put("activeMinutes", opMinutes);
            om.put("activeHours", Math.round(opMinutes / 60.0 * 10) / 10.0);
            operationData.add(om);
        }

        // Distinct operators
        long operatorCount = records.stream()
            .map(WipTracking::getOperatorId).filter(Objects::nonNull).distinct().count();

        Map<String, Object> result = new HashMap<>();
        result.put("machineId", machineId);
        result.put("machineName", machineName);
        result.put("machineType", machine != null ? machine.getType() : "");
        result.put("date", date.toString());
        result.put("activeMinutes", activeMinutes);
        result.put("idleMinutes", idleMinutes);
        result.put("activeHours", Math.round(activeMinutes / 60.0 * 10) / 10.0);
        result.put("idleHours", Math.round(idleMinutes / 60.0 * 10) / 10.0);
        result.put("utilisationPercent", workingDayMinutes > 0 ? Math.round(activeMinutes * 100.0 / workingDayMinutes) : 0);
        result.put("operatorCount", operatorCount);
        result.put("operationData", operationData);
        return result;
    }
}
