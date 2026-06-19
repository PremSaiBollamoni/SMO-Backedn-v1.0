package com.cutm.smo.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class ShiftBreakDto {
    private Long breakId;
    private String breakName;
    private LocalTime startTime;
    private LocalTime endTime;
}
