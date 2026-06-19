package com.cutm.smo.dto;

import lombok.Data;
import java.time.LocalTime;
import java.util.List;

@Data
public class ShiftTemplateDto {
    private Long templateId;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private List<ShiftBreakDto> breaks;
}
