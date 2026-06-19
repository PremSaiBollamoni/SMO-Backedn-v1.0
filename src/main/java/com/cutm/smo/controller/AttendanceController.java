package com.cutm.smo.controller;

import com.cutm.smo.dto.AttendanceRecordDto;
import com.cutm.smo.dto.CheckInRequest;
import com.cutm.smo.dto.CheckOutRequest;
import com.cutm.smo.dto.MapQrRequest;
import com.cutm.smo.services.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /** Map a temp QR card to an employee for today before check-in. */
    @PostMapping("/map-qr")
    public ResponseEntity<Map<String, Object>> mapQr(@RequestBody MapQrRequest req) {
        var mapping = attendanceService.mapQr(req);
        return ResponseEntity.ok(Map.of(
                "mappingId", mapping.getMappingId(),
                "qrToken", mapping.getQrToken(),
                "empId", mapping.getEmpId(),
                "mappingDate", mapping.getMappingDate().toString()
        ));
    }

    /** Resolve which employee holds a temp QR today (preview). */
    @GetMapping("/resolve-qr")
    public ResponseEntity<Map<String, Object>> resolveQr(@RequestParam String qrToken) {
        Long empId = attendanceService.resolveQrToEmployee(qrToken);
        return ResponseEntity.ok(Map.of("empId", empId));
    }

    /** Check in using scanned temp QR + machine QR. */
    @PostMapping("/checkin")
    public AttendanceRecordDto checkIn(@RequestBody CheckInRequest req) {
        return attendanceService.checkIn(req);
    }

    /** Check out by scanning the temp QR again. */
    @PostMapping("/checkout")
    public AttendanceRecordDto checkOut(@RequestBody CheckOutRequest req) {
        return attendanceService.checkOut(req);
    }

    /** Today's full attendance list. */
    @GetMapping("/today")
    public List<AttendanceRecordDto> getTodayAttendance() {
        return attendanceService.getTodayAttendance();
    }

    /** Free all temp QR mappings at end of shift/day. */
    @PostMapping("/free-qrs")
    public ResponseEntity<Map<String, Object>> freeAllQrs() {
        int count = attendanceService.freeAllQrs();
        return ResponseEntity.ok(Map.of("freed", count));
    }
}
