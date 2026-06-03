package com.cutm.smo.controller;

import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import com.cutm.smo.repository.HourlyTargetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Controller — NEW endpoints only.
 * Does NOT modify any existing controller or service.
 */
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired private WipTrackingRepository wipTrackingRepository;
    @Autowired private EmployeeInfoRepository employeeInfoRepository;
    @Autowired private MachineRepository machineRepository;
    @Autowired private HourlyTargetRepository hourlyTargetRepository;
    @Autowired private RoutingRepository routingRepository;

    // ─── Routings ───────────────────────────────────────────────────────────

    /** GET /api/analytics/routings — list approved routings */
    @GetMapping("/routings")
    public List<Map<String, Object>> getRoutings() {
        return routingRepository.findAll().stream()
            .filter(r -> "APPROVED".equalsIgnoreCase(r.getApprovalStatus()))
            .map(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("routingId", r.getRoutingId());
                m.put("productId", r.getProductId());
                m.put("version", r.getVersion());
                m.put("status", r.getStatus());
                return m;
            })
            .collect(Collectors.toList());
    }

    // ─── Operations ─────────────────────────────────────────────────────────

    /**
     * GET /api/analytics/routing/{routingId}/operations?date=YYYY-MM-DD
     * Returns all operations in a routing with actual vs target stats for the date.
     */
    @GetMapping("/routing/{routingId}/operations")
    public List<Map<String, Object>> getOperationsForRouting(
            @PathVariable Long routingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

        // Get all wip records for this date
        List<WipTracking> dayWip = wipTrackingRepository.findAll().stream()
            .filter(w -> w.getStartTime() != null
                && !w.getStartTime().isBefore(dayStart)
                && !w.getStartTime().isAfter(dayEnd))
            .collect(Collectors.toList());

        // Get routing steps to find operations in this routing
        // We use wiptracking grouped by operation_id
        Map<Long, List<WipTracking>> byOperation = dayWip.stream()
            .filter(w -> w.getOperationId() != null)
            .collect(Collectors.groupingBy(WipTracking::getOperationId));

        // Get all unique operation IDs from wip for this routing
        // Since we don't have a direct routing→operation join here, return all operations that have wip data
        // plus the hourly targets
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<Long, List<WipTracking>> entry : byOperation.entrySet()) {
            Long opId = entry.getKey();
            List<WipTracking> opWip = entry.getValue();

            int totalQty = opWip.stream().mapToInt(w -> w.getQty() != null ? w.getQty() : 0).sum();
            int completedCount = (int) opWip.stream().filter(w -> "COMPLETED".equalsIgnoreCase(w.getStatus())).count();
            int activeCount = (int) opWip.stream().filter(w -> "IN_PROGRESS".equalsIgnoreCase(w.getStatus())).count();

            Optional<HourlyTarget> target = hourlyTargetRepository.findByOperationId(opId);
            int targetPerHour = target.map(HourlyTarget::getTargetPerHour).orElse(30);
            String opName = target.map(HourlyTarget::getOperationName).orElse("Operation " + opId);

            // Estimate hours worked (8-hour shift)
            int expectedTotal = targetPerHour * 8;

            Map<String, Object> m = new HashMap<>();
            m.put("operationId", opId);
            m.put("operationName", opName);
            m.put("totalQty", totalQty);
            m.put("completedCount", completedCount);
            m.put("activeCount", activeCount);
            m.put("targetPerHour", targetPerHour);
            m.put("expectedDayTotal", expectedTotal);
            m.put("achievementPercent", expectedTotal > 0 ? (totalQty * 100 / expectedTotal) : 0);
            result.add(m);
        }

        return result;
    }

    /**
     * GET /api/analytics/operation/{operationId}/employees?date=YYYY-MM-DD
     * Returns employees who worked on this operation on the given date with their stats.
     */
    @GetMapping("/operation/{operationId}/employees")
    public List<Map<String, Object>> getEmployeesForOperation(
            @PathVariable Long operationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

        List<WipTracking> opWip = wipTrackingRepository.findAll().stream()
            .filter(w -> operationId.equals(w.getOperationId())
                && w.getStartTime() != null
                && !w.getStartTime().isBefore(dayStart)
                && !w.getStartTime().isAfter(dayEnd))
            .collect(Collectors.toList());

        Map<Long, List<WipTracking>> byEmployee = opWip.stream()
            .filter(w -> w.getOperatorId() != null)
            .collect(Collectors.groupingBy(WipTracking::getOperatorId));

        Optional<HourlyTarget> target = hourlyTargetRepository.findByOperationId(operationId);
        int targetPerHour = target.map(HourlyTarget::getTargetPerHour).orElse(30);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<WipTracking>> entry : byEmployee.entrySet()) {
            Long empId = entry.getKey();
            List<WipTracking> empWip = entry.getValue();

            int totalQty = empWip.stream().mapToInt(w -> w.getQty() != null ? w.getQty() : 0).sum();
            String empName = employeeInfoRepository.findById(empId)
                .map(EmployeeInfo::getEmpName).orElse("Employee " + empId);

            Map<String, Object> m = new HashMap<>();
            m.put("employeeId", empId);
            m.put("employeeName", empName);
            m.put("totalQty", totalQty);
            m.put("targetPerHour", targetPerHour);
            result.add(m);
        }
        return result;
    }

    // ─── Employee Stats ──────────────────────────────────────────────────────

    /**
     * GET /api/analytics/employees?date=YYYY-MM-DD
     * Returns all employees with summary stats for the date.
     */
    @GetMapping("/employees")
    public List<Map<String, Object>> getAllEmployeeStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

        List<WipTracking> dayWip = wipTrackingRepository.findAll().stream()
            .filter(w -> w.getStartTime() != null
                && !w.getStartTime().isBefore(dayStart)
                && !w.getStartTime().isAfter(dayEnd)
                && w.getOperatorId() != null)
            .collect(Collectors.toList());

        Map<Long, List<WipTracking>> byEmployee = dayWip.stream()
            .collect(Collectors.groupingBy(WipTracking::getOperatorId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<WipTracking>> entry : byEmployee.entrySet()) {
            Long empId = entry.getKey();
            List<WipTracking> empWip = entry.getValue();
            int totalQty = empWip.stream().mapToInt(w -> w.getQty() != null ? w.getQty() : 0).sum();
            int completed = (int) empWip.stream().filter(w -> "COMPLETED".equalsIgnoreCase(w.getStatus())).count();
            String empName = employeeInfoRepository.findById(empId)
                .map(EmployeeInfo::getEmpName).orElse("Employee " + empId);

            Map<String, Object> m = new HashMap<>();
            m.put("employeeId", empId);
            m.put("employeeName", empName);
            m.put("totalQty", totalQty);
            m.put("completedJobs", completed);
            result.add(m);
        }
        return result;
    }

    /**
     * GET /api/analytics/employee/{empId}/stats?date=YYYY-MM-DD
     * Returns hourly + overall stats for a specific employee.
     */
    @GetMapping("/employee/{empId}/stats")
    public Map<String, Object> getEmployeeStats(
            @PathVariable Long empId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

        List<WipTracking> empWip = wipTrackingRepository.findAll().stream()
            .filter(w -> empId.equals(w.getOperatorId())
                && w.getStartTime() != null
                && !w.getStartTime().isBefore(dayStart)
                && !w.getStartTime().isAfter(dayEnd))
            .collect(Collectors.toList());

        String empName = employeeInfoRepository.findById(empId)
            .map(EmployeeInfo::getEmpName).orElse("Employee " + empId);

        // Hourly breakdown (hour 0-23 → qty)
        Map<Integer, Integer> hourlyQty = new LinkedHashMap<>();
        for (int h = 6; h <= 20; h++) hourlyQty.put(h, 0);
        for (WipTracking w : empWip) {
            int hour = w.getStartTime().getHour();
            int qty = w.getQty() != null ? w.getQty() : 0;
            hourlyQty.merge(hour, qty, Integer::sum);
        }

        // Per-operation breakdown
        Map<Long, Integer> opQty = new LinkedHashMap<>();
        for (WipTracking w : empWip) {
            if (w.getOperationId() != null) {
                opQty.merge(w.getOperationId(), w.getQty() != null ? w.getQty() : 0, Integer::sum);
            }
        }

        List<Map<String, Object>> perOperation = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : opQty.entrySet()) {
            Optional<HourlyTarget> ht = hourlyTargetRepository.findByOperationId(e.getKey());
            Map<String, Object> op = new HashMap<>();
            op.put("operationId", e.getKey());
            op.put("operationName", ht.map(HourlyTarget::getOperationName).orElse("Op " + e.getKey()));
            op.put("qty", e.getValue());
            op.put("targetPerHour", ht.map(HourlyTarget::getTargetPerHour).orElse(30));
            perOperation.add(op);
        }

        // Overall stats
        int totalQty = empWip.stream().mapToInt(w -> w.getQty() != null ? w.getQty() : 0).sum();
        int completedJobs = (int) empWip.stream().filter(w -> "COMPLETED".equalsIgnoreCase(w.getStatus())).count();
        long totalMinutes = empWip.stream()
            .filter(w -> w.getEndTime() != null)
            .mapToLong(w -> java.time.Duration.between(w.getStartTime(), w.getEndTime()).toMinutes())
            .sum();
        double avgMinPerJob = completedJobs > 0 ? (double) totalMinutes / completedJobs : 0;

        // Hourly target for most common operation
        int targetPerHour = 30;
        if (!opQty.isEmpty()) {
            Long topOp = opQty.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
            if (topOp != null) {
                targetPerHour = hourlyTargetRepository.findByOperationId(topOp)
                    .map(HourlyTarget::getTargetPerHour).orElse(30);
            }
        }

        // Build hourly data list
        List<Map<String, Object>> hourlyData = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : hourlyQty.entrySet()) {
            Map<String, Object> h = new HashMap<>();
            h.put("hour", e.getKey());
            h.put("label", String.format("%02d:00", e.getKey()));
            h.put("qty", e.getValue());
            h.put("target", targetPerHour);
            hourlyData.add(h);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("employeeId", empId);
        result.put("employeeName", empName);
        result.put("date", targetDate.toString());
        result.put("totalQty", totalQty);
        result.put("completedJobs", completedJobs);
        result.put("totalMinutesWorked", totalMinutes);
        result.put("avgMinPerJob", Math.round(avgMinPerJob * 10.0) / 10.0);
        result.put("hourlyData", hourlyData);
        result.put("perOperation", perOperation);
        return result;
    }

    // ─── Machine Utilisation ─────────────────────────────────────────────────

    /**
     * GET /api/analytics/machines?date=YYYY-MM-DD
     * Returns all machines with utilisation summary.
     */
    @GetMapping("/machines")
    public List<Map<String, Object>> getMachines(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

        List<WipTracking> dayWip = wipTrackingRepository.findAll().stream()
            .filter(w -> w.getStartTime() != null
                && !w.getStartTime().isBefore(dayStart)
                && !w.getStartTime().isAfter(dayEnd)
                && w.getMachineId() != null)
            .collect(Collectors.toList());

        Map<String, List<WipTracking>> byMachine = dayWip.stream()
            .filter(w -> w.getMachineId() != null)
            .collect(Collectors.groupingBy(w -> String.valueOf(w.getMachineId())));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Machine machine : machineRepository.findAll()) {
            List<WipTracking> mWip = byMachine.getOrDefault(machine.getMachineId(), Collections.emptyList());
            long activeMinutes = mWip.stream()
                .filter(w -> w.getEndTime() != null)
                .mapToLong(w -> java.time.Duration.between(w.getStartTime(), w.getEndTime()).toMinutes())
                .sum();
            int jobs = mWip.size();

            Map<String, Object> m = new HashMap<>();
            m.put("machineId", machine.getMachineId());
            m.put("machineName", machine.getName());
            m.put("machineType", machine.getType());
            m.put("status", machine.getStatus());
            m.put("activeMinutes", activeMinutes);
            m.put("activeHours", Math.round(activeMinutes / 60.0 * 10.0) / 10.0);
            m.put("idleMinutes", Math.max(0, 480 - activeMinutes)); // 8-hour shift
            m.put("jobsCount", jobs);
            result.add(m);
        }
        return result;
    }

    /**
     * GET /api/analytics/machine/{machineId}/stats?date=YYYY-MM-DD
     * Returns detailed utilisation stats for a machine.
     */
    @GetMapping("/machine/{machineId}/stats")
    public Map<String, Object> getMachineStats(
            @PathVariable String machineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

        Machine machine = machineRepository.findById(machineId).orElse(null);
        String machineName = machine != null ? machine.getName() : "Machine " + machineId;

        List<WipTracking> mWip = wipTrackingRepository.findAll().stream()
            .filter(w -> w.getMachineId() != null && machineId.equals(String.valueOf(w.getMachineId()))
                && w.getStartTime() != null
                && !w.getStartTime().isBefore(dayStart)
                && !w.getStartTime().isAfter(dayEnd))
            .collect(Collectors.toList());

        long activeMinutes = mWip.stream()
            .filter(w -> w.getEndTime() != null)
            .mapToLong(w -> java.time.Duration.between(w.getStartTime(), w.getEndTime()).toMinutes())
            .sum();
        long idleMinutes = Math.max(0, 480 - activeMinutes);

        // Per-operation breakdown
        Map<Long, Long> opMinutes = new LinkedHashMap<>();
        for (WipTracking w : mWip) {
            if (w.getOperationId() != null && w.getEndTime() != null) {
                long mins = java.time.Duration.between(w.getStartTime(), w.getEndTime()).toMinutes();
                opMinutes.merge(w.getOperationId(), mins, Long::sum);
            }
        }

        List<Map<String, Object>> perOperation = new ArrayList<>();
        for (Map.Entry<Long, Long> e : opMinutes.entrySet()) {
            Optional<HourlyTarget> ht = hourlyTargetRepository.findByOperationId(e.getKey());
            Map<String, Object> op = new HashMap<>();
            op.put("operationId", e.getKey());
            op.put("operationName", ht.map(HourlyTarget::getOperationName).orElse("Op " + e.getKey()));
            op.put("minutes", e.getValue());
            op.put("hours", Math.round(e.getValue() / 60.0 * 10.0) / 10.0);
            perOperation.add(op);
        }

        // Unique operators
        long uniqueOperators = mWip.stream()
            .filter(w -> w.getOperatorId() != null)
            .map(WipTracking::getOperatorId)
            .distinct().count();

        // Hourly usage
        Map<Integer, Long> hourlyUsage = new LinkedHashMap<>();
        for (int h = 6; h <= 20; h++) hourlyUsage.put(h, 0L);
        for (WipTracking w : mWip) {
            if (w.getEndTime() != null) {
                int hour = w.getStartTime().getHour();
                long mins = java.time.Duration.between(w.getStartTime(), w.getEndTime()).toMinutes();
                hourlyUsage.merge(hour, mins, Long::sum);
            }
        }

        List<Map<String, Object>> hourlyData = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : hourlyUsage.entrySet()) {
            Map<String, Object> h = new HashMap<>();
            h.put("hour", e.getKey());
            h.put("label", String.format("%02d:00", e.getKey()));
            h.put("minutes", e.getValue());
            hourlyData.add(h);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("machineId", machineId);
        result.put("machineName", machineName);
        result.put("date", targetDate.toString());
        result.put("activeMinutes", activeMinutes);
        result.put("activeHours", Math.round(activeMinutes / 60.0 * 10.0) / 10.0);
        result.put("idleMinutes", idleMinutes);
        result.put("idleHours", Math.round(idleMinutes / 60.0 * 10.0) / 10.0);
        result.put("utilizationPercent", activeMinutes > 0 ? (int)(activeMinutes * 100 / 480) : 0);
        result.put("uniqueOperators", uniqueOperators);
        result.put("perOperation", perOperation);
        result.put("hourlyData", hourlyData);
        return result;
    }

    /** GET /api/analytics/hourly-targets — list all hourly targets */
    @GetMapping("/hourly-targets")
    public List<HourlyTarget> getHourlyTargets() {
        return hourlyTargetRepository.findAll();
    }

    /** PUT /api/analytics/hourly-targets/{id} — update a target */
    @PutMapping("/hourly-targets/{id}")
    public HourlyTarget updateHourlyTarget(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body) {
        HourlyTarget target = hourlyTargetRepository.findById(id)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Target not found"));
        if (body.containsKey("targetPerHour")) {
            target.setTargetPerHour(Integer.valueOf(body.get("targetPerHour").toString()));
        }
        return hourlyTargetRepository.save(target);
    }
}
