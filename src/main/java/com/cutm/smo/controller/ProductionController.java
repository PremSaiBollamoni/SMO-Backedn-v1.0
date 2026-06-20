package com.cutm.smo.controller;

import com.cutm.smo.dto.EmployeeEfficiencyDto;
import com.cutm.smo.dto.StationProductionDto;
import java.time.LocalDate;
import com.cutm.smo.services.ProductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/production")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;

    @GetMapping("/efficiency/today")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR')")
    public List<EmployeeEfficiencyDto> getEfficiencyToday() {
        return productionService.getEfficiencyToday();
    }

    @GetMapping("/efficiency/date")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR')")
    public List<EmployeeEfficiencyDto> getEfficiencyByDate(@RequestParam String date) {
        return productionService.getEfficiencyByDate(LocalDate.parse(date));
    }

    @GetMapping("/employee/{empId}/slots")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR')")
    public List<StationProductionDto> getEmployeeSlots(@PathVariable Long empId, @RequestParam String date) {
        return productionService.getEmployeeSlotsByDate(empId, LocalDate.parse(date));
    }

    @GetMapping("/station/{wsId}/today")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR')")
    public List<StationProductionDto> getStationProductionToday(@PathVariable Long wsId) {
        return productionService.getStationProductionToday(wsId);
    }
}
