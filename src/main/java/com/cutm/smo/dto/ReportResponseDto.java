package com.cutm.smo.dto;

import lombok.Data;

@Data
public class ReportResponseDto {
    private String insights;
    private String generatedAt;
    private Double overallEfficiency;
    private Integer totalOperators;
    private Integer totalPieces;
}
