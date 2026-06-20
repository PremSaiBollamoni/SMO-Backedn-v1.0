package com.cutm.smo.controller;

import com.cutm.smo.dto.ReportResponseDto;
import com.cutm.smo.services.GeminiService;
import com.cutm.smo.services.ProductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ProductionService productionService;
    private final GeminiService geminiService;

    @GetMapping("/generate")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR')")
    public ReportResponseDto generateReport() {
        var data = productionService.getEfficiencyToday();

        String insights = geminiService.generateProductionReport(data);

        double overallEff = data.stream()
            .mapToDouble(e -> e.getEfficiencyPct() != null ? e.getEfficiencyPct() : 0)
            .average().orElse(0);

        int totalPieces = data.stream()
            .mapToInt(e -> e.getTotalPieces() != null ? e.getTotalPieces() : 0)
            .sum();

        ReportResponseDto dto = new ReportResponseDto();
        dto.setInsights(insights);
        dto.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
        dto.setOverallEfficiency(Math.round(overallEff * 10.0) / 10.0);
        dto.setTotalOperators(data.size());
        dto.setTotalPieces(totalPieces);
        return dto;
    }
}
