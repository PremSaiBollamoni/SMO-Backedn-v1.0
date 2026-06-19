package com.cutm.smo.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class AddBreakRequest {
    private String breakName;
    private LocalTime startTime;
    private LocalTime endTime;
}
