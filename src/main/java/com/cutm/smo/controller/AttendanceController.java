package com.cutm.smo.controller;

import com.cutm.smo.dto.AttendanceRecordDto;
import com.cutm.smo.dto.CheckInRequest;
import com.cutm.smo.dto.CheckOutRequest;
import com.cutm.smo.dto.MapQrRequest;
import com.cutm.smo.services.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/map-qr")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR')")
    public ResponseEntity<Map<String, Object>> mapQr(@RequestBody MapQrRequest req) {
        log.debug("Mapping QR for employee: {}", req.getEmpId());
        var mapping = attendanceService.mapQr(req);
        return ResponseEntity.ok(Map.of(
                "mappingId", mapping.getMappingId(),
                "qrToken", mapping.getQrToken(),
                "empId", mapping.getEmpId(),
                "mappingDate", mapping.getMappingDate().toString()
        ));
    }

    @GetMapping("/resolve-qr")
    @PreAuthorize("hasAnyRole('OPERATOR', 'SUPERVISOR', 'HR')")
    public ResponseEntity<Map<String, Object>> resolveQr(@RequestParam String qrToken) {
        log.debug("Resolving QR token");
        Long empId = attendanceService.resolveQrToEmployee(qrToken);
        return ResponseEntity.ok(Map.of("empId", empId));
    }

    @PostMapping("/checkin")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR', 'OPERATOR', 'CUTTER', 'STITCHER', 'PACKAGER', 'IRONING')")
    public AttendanceRecordDto checkIn(@RequestBody CheckInRequest req) {
        log.debug("Check-in request received");
        return attendanceService.checkIn(req);
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR', 'OPERATOR', 'CUTTER', 'STITCHER', 'PACKAGER', 'IRONING')")
    public AttendanceRecordDto checkOut(@RequestBody CheckOutRequest req) {
        log.debug("Check-out request received");
        return attendanceService.checkOut(req);
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR')")
    public List<AttendanceRecordDto> getTodayAttendance() {
        log.debug("Fetching today's attendance");
        return attendanceService.getTodayAttendance();
    }

    @GetMapping("/date")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR')")
    public List<AttendanceRecordDto> getAttendanceByDate(@RequestParam String date) {
        log.debug("Fetching attendance for date: {}", date);
        return attendanceService.getAttendanceByDate(java.time.LocalDate.parse(date));
    }

    @PostMapping("/free-qrs")
    @PreAuthorize("hasAnyRole('HR')")
    public ResponseEntity<Map<String, Object>> freeAllQrs() {
        log.info("Freeing all QR mappings");
        int count = attendanceService.freeAllQrs();
        return ResponseEntity.ok(Map.of("freed", count));
    }
}
