package com.cutm.smo.dto;

import lombok.Data;
import java.util.List;

@Data
public class EmployeeEfficiencyDto {
    private Long empId;
    private String empName;
    private Integer totalPieces;
    private Integer productiveSlots;
    private Double efficiencyPct;
    private List<String> operations;
}
