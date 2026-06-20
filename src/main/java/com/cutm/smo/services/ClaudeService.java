package com.cutm.smo.services;

import com.cutm.smo.dto.EmployeeEfficiencyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ClaudeService {

    @Value("${anthropic.api.key:}")
    private String apiKey;

    private static final String CLAUDE_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-haiku-4-5-20251001";
    private final RestTemplate restTemplate = new RestTemplate();

    public String generateProductionReport(List<EmployeeEfficiencyDto> data) {
        log.info("Claude API key loaded: {} chars, starts with: {}", apiKey == null ? 0 : apiKey.length(), apiKey == null || apiKey.length() < 10 ? "EMPTY" : apiKey.substring(0, 10));
        if (apiKey == null || apiKey.isBlank()) {
            return "API key not configured. Set ANTHROPIC_API_KEY environment variable.";
        }

        String prompt = buildPrompt(data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = Map.of(
            "model", MODEL,
            "max_tokens", 2048,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(CLAUDE_URL, request, Map.class);
            List<?> content = (List<?>) response.getBody().get("content");
            Map<?, ?> first = (Map<?, ?>) content.get(0);
            return (String) first.get("text");
        } catch (Exception e) {
            log.error("Claude API call failed: {}", e.getMessage());
            return "Failed to generate AI insights: " + e.getMessage();
        }
    }

    private String buildPrompt(List<EmployeeEfficiencyDto> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a garment factory production analyst. Analyze today's (").append(LocalDate.now()).append(") production data and generate a detailed professional report.\n\n");
        sb.append("PRODUCTION DATA:\n");
        int totalPieces = 0;
        for (EmployeeEfficiencyDto emp : data) {
            sb.append("- ").append(emp.getEmpName())
              .append(" | Operations: ").append(String.join(", ", emp.getOperations()))
              .append(" | Pieces: ").append(emp.getTotalPieces())
              .append(" | Slots: ").append(emp.getProductiveSlots())
              .append(" | Efficiency: ").append(emp.getEfficiencyPct()).append("%\n");
            totalPieces += emp.getTotalPieces();
        }
        double avgEff = data.stream().mapToDouble(EmployeeEfficiencyDto::getEfficiencyPct).average().orElse(0);
        sb.append("\nTotal Pieces: ").append(totalPieces);
        sb.append("\nOverall Line Efficiency: ").append(String.format("%.1f", avgEff)).append("%\n\n");
        sb.append("Generate a report with exactly these 5 sections. STRICT RULES:\n");
        sb.append("- NO markdown, NO asterisks (*), NO hashtags (#), NO bullet dashes (-), NO bold (**text**)\n");
        sb.append("- Use plain text only\n");
        sb.append("- Start each section with the label on its own line in ALL CAPS followed by a colon, then the content on the next line\n");
        sb.append("- Keep each section to 3-4 sentences maximum\n\n");
        sb.append("Sections:\n");
        sb.append("EXECUTIVE SUMMARY:\nTOP PERFORMERS:\nAREAS OF CONCERN:\nOPERATIONAL INSIGHTS:\nRECOMMENDATIONS FOR TOMORROW:\n\n");
        sb.append("Be specific with numbers. Professional tone.");
        return sb.toString();
    }
}
