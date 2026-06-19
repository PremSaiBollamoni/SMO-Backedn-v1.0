package com.cutm.smo.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class CreateShiftTemplateRequest {
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
}
