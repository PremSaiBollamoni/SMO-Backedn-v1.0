package com.cutm.smo.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AttendanceRecordDto {
    private Long attId;
    private Long empId;
    private String empName;
    private String tempQrToken;
    private String machineCode;
    private String shiftName;
    private LocalDate attDate;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private String status;
}
