package com.cutm.smo.services;

import com.cutm.smo.dto.EmployeeEfficiencyDto;
import com.cutm.smo.dto.StationProductionDto;
import com.cutm.smo.dto.StationProductionDto.SlotDto;
import com.cutm.smo.models.ProductionLog;
import com.cutm.smo.repositories.EmployeeRepository;
import com.cutm.smo.repositories.ProductionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionService {

    private final ProductionLogRepository productionLogRepo;
    private final EmployeeRepository employeeRepo;

    private static String slotLabel(int hour) {
        // Legacy 1-8 slots
        if (hour <= 8) {
            String[] labels = {"9:00-10:00","10:00-11:00","11:00-12:00","12:00-1:00","1:30-2:30","2:30-3:30","3:30-4:30","4:30-5:30"};
            return hour >= 1 && hour <= 8 ? labels[hour - 1] : "Slot " + hour;
        }
        // Actual hour-based slots (e.g. 10 → "10:00-11:00")
        int end = hour + 1;
        return String.format("%d:00-%d:00", hour, end);
    }

    public List<EmployeeEfficiencyDto> getEfficiencyToday() {
        return getEfficiencyByDate(LocalDate.now());
    }

    public List<EmployeeEfficiencyDto> getEfficiencyByDate(LocalDate date) {
        List<ProductionLog> logs = productionLogRepo.findAllByDate(date);
        Map<Long, List<ProductionLog>> byEmp = logs.stream()
                .collect(Collectors.groupingBy(ProductionLog::getEmpId));
        List<EmployeeEfficiencyDto> result = new ArrayList<>();

        for (Map.Entry<Long, List<ProductionLog>> entry : byEmp.entrySet()) {
            Long empId = entry.getKey();
            List<ProductionLog> empLogs = entry.getValue();
            EmployeeEfficiencyDto dto = new EmployeeEfficiencyDto();
            dto.setEmpId(empId);
            employeeRepo.findById(empId).ifPresent(e -> dto.setEmpName(e.getEmpName()));

            int totalPieces = empLogs.stream().mapToInt(ProductionLog::getPiecesProduced).sum();
            dto.setTotalPieces(totalPieces);
            dto.setProductiveSlots(empLogs.size());

            // Group by ws to get operations and their targets
            Map<Long, List<ProductionLog>> byWs = empLogs.stream().collect(Collectors.groupingBy(p -> p.getWorkstation().getWsId()));
            List<String> ops = new ArrayList<>();
            double totalTarget = 0;
            for (List<ProductionLog> wsLogs : byWs.values()) {
                var op = wsLogs.get(0).getWorkstation().getOperation();
                if (op != null) {
                    ops.add(op.getOpName());
                    totalTarget += wsLogs.size() * (op.getTargetPcs() != null ? op.getTargetPcs() : 50);
                }
            }
            dto.setOperations(ops);
            double eff = totalTarget > 0 ? (totalPieces * 100.0 / totalTarget) : 0;
            dto.setEfficiencyPct(Math.round(eff * 10.0) / 10.0);
            result.add(dto);
        }
        result.sort((a, b) -> Double.compare(b.getEfficiencyPct(), a.getEfficiencyPct()));
        return result;
    }

    public List<StationProductionDto> getEmployeeSlotsByDate(Long empId, LocalDate date) {
        List<ProductionLog> logs = productionLogRepo.findAllByDate(date).stream()
                .filter(l -> l.getEmpId().equals(empId))
                .collect(Collectors.toList());

        if (logs.isEmpty()) return Collections.emptyList();

        String empName = employeeRepo.findById(empId).map(e -> e.getEmpName()).orElse("Unknown");

        // Group by workstation so each operation is a separate section
        Map<Long, List<ProductionLog>> byWs = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getWorkstation().getWsId()));

        List<StationProductionDto> result = new ArrayList<>();
        for (List<ProductionLog> wsLogs : byWs.values()) {
            StationProductionDto dto = new StationProductionDto();
            dto.setEmpId(empId);
            dto.setEmpName(empName);

            var ws = wsLogs.get(0).getWorkstation();
            var op = ws.getOperation();
            dto.setOpName(op != null ? op.getOpName() : ws.getWsCode());
            Integer targetPcs = op != null ? op.getTargetPcs() : 50;
            dto.setTargetPcs(targetPcs);

            List<SlotDto> slots = wsLogs.stream()
                    .sorted(Comparator.comparing(ProductionLog::getHourSlot))
                    .map(log -> {
                        SlotDto slot = new SlotDto();
                        slot.setHourSlot(log.getHourSlot());
                        slot.setTimeLabel(slotLabel(log.getHourSlot()));
                        slot.setPiecesProduced(log.getPiecesProduced());
                        double slotEff = targetPcs > 0 ? (log.getPiecesProduced() * 100.0 / targetPcs) : 0;
                        slot.setSlotEfficiencyPct(Math.round(slotEff * 10.0) / 10.0);
                        return slot;
                    }).collect(Collectors.toList());
            dto.setSlots(slots);

            int totalPieces = slots.stream().mapToInt(SlotDto::getPiecesProduced).sum();
            double dayEff = (targetPcs > 0 && !slots.isEmpty())
                    ? (totalPieces * 100.0 / (slots.size() * targetPcs)) : 0;
            dto.setTotalPieces(totalPieces);
            dto.setEfficiencyPct(Math.round(dayEff * 10.0) / 10.0);
            result.add(dto);
        }
        return result;
    }

    public List<StationProductionDto> getStationProductionToday(Long wsId) {
        List<ProductionLog> logs = productionLogRepo.findByWsIdAndDate(wsId, LocalDate.now());

        Map<Long, List<ProductionLog>> byEmp = logs.stream()
                .collect(Collectors.groupingBy(ProductionLog::getEmpId));

        List<StationProductionDto> result = new ArrayList<>();

        for (Map.Entry<Long, List<ProductionLog>> entry : byEmp.entrySet()) {
            Long empId = entry.getKey();
            List<ProductionLog> empLogs = entry.getValue();

            StationProductionDto dto = new StationProductionDto();
            dto.setEmpId(empId);
            employeeRepo.findById(empId).ifPresent(e -> dto.setEmpName(e.getEmpName()));

            // Get operation info from first log's workstation
            var ws = empLogs.get(0).getWorkstation();
            var op = ws.getOperation();
            dto.setOpName(op != null ? op.getOpName() : ws.getWsCode());
            Integer targetPcs = op != null ? op.getTargetPcs() : 50;
            dto.setTargetPcs(targetPcs);

            List<SlotDto> slots = empLogs.stream()
                    .sorted(Comparator.comparing(ProductionLog::getHourSlot))
                    .map(log -> {
                        SlotDto slot = new SlotDto();
                        slot.setHourSlot(log.getHourSlot());
                        slot.setTimeLabel(slotLabel(log.getHourSlot()));
                        slot.setPiecesProduced(log.getPiecesProduced());
                        double slotEff = targetPcs > 0 ? (log.getPiecesProduced() * 100.0 / targetPcs) : 0;
                        slot.setSlotEfficiencyPct(Math.round(slotEff * 10.0) / 10.0);
                        return slot;
                    }).collect(Collectors.toList());

            dto.setSlots(slots);

            int totalPieces = slots.stream().mapToInt(SlotDto::getPiecesProduced).sum();
            int productiveSlots = slots.size();
            double dayEff = (productiveSlots > 0 && targetPcs > 0)
                    ? (totalPieces * 100.0 / (productiveSlots * targetPcs)) : 0;

            dto.setTotalPieces(totalPieces);
            dto.setEfficiencyPct(Math.round(dayEff * 10.0) / 10.0);

            result.add(dto);
        }

        return result;
    }
}
