package com.cutm.smo.services;

import com.cutm.smo.dto.AttendanceRecordDto;
import com.cutm.smo.dto.CheckInRequest;
import com.cutm.smo.dto.CheckOutRequest;
import com.cutm.smo.dto.MapQrRequest;
import com.cutm.smo.models.Attendance;
import com.cutm.smo.models.ShiftTemplate;
import com.cutm.smo.models.TempQrMapping;
import com.cutm.smo.repositories.AttendanceRepository;
import com.cutm.smo.repositories.EmployeeInfoRepository;
import com.cutm.smo.repositories.ShiftTemplateRepository;
import com.cutm.smo.repositories.TempQrMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final TempQrMappingRepository tempQrRepo;
    private final AttendanceRepository attendanceRepo;
    private final ShiftTemplateRepository shiftTemplateRepo;
    private final EmployeeInfoRepository employeeInfoRepo;

    /** Map a temp QR card to an employee for today. */
    @Transactional
    public TempQrMapping mapQr(MapQrRequest req) {
        LocalDate today = LocalDate.now();
        if (tempQrRepo.existsByQrTokenAndMappingDateAndFreedFalse(req.getQrToken(), today)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    req.getQrToken() + " is already assigned today. Free it first.");
        }
        if (!employeeInfoRepo.existsById(req.getEmpId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        }
        TempQrMapping m = new TempQrMapping();
        m.setQrToken(req.getQrToken());
        m.setEmpId(req.getEmpId());
        m.setMappingDate(today);
        m.setMappedBy(req.getMappedBy());
        return tempQrRepo.save(m);
    }

    /** Check in: temp QR must already be mapped to an employee today. */
    @Transactional
    public AttendanceRecordDto checkIn(CheckInRequest req) {
        LocalDate today = LocalDate.now();

        TempQrMapping mapping = tempQrRepo
                .findByQrTokenAndMappingDateAndFreedFalse(req.getTempQrToken(), today)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        req.getTempQrToken() + " is not mapped to any employee today. Scan & assign first."));

        // Prevent duplicate check-in
        attendanceRepo.findByTempQrTokenAndAttDateAndStatus(req.getTempQrToken(), today, "CHECKED_IN")
                .ifPresent(a -> { throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Already checked in. Scan to check out."); });

        ShiftTemplate shift = req.getShiftTemplateId() != null
                ? shiftTemplateRepo.findById(req.getShiftTemplateId()).orElse(null)
                : null;

        Attendance att = new Attendance();
        att.setEmpId(mapping.getEmpId());
        att.setTempQrToken(req.getTempQrToken());
        att.setMachineCode(req.getMachineCode());
        att.setShiftTemplate(shift);
        att.setAttDate(today);
        att.setCheckIn(LocalDateTime.now());
        att.setStatus("CHECKED_IN");
        att.setMarkedBy(req.getMarkedBy());

        return toDto(attendanceRepo.save(att));
    }

    /** Check out: scan same temp QR to close the open check-in. */
    @Transactional
    public AttendanceRecordDto checkOut(CheckOutRequest req) {
        LocalDate today = LocalDate.now();

        Attendance att = attendanceRepo
                .findByTempQrTokenAndAttDateAndStatus(req.getTempQrToken(), today, "CHECKED_IN")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active check-in found for " + req.getTempQrToken()));

        att.setCheckOut(LocalDateTime.now());
        att.setStatus("CHECKED_OUT");
        att.setMarkedBy(req.getMarkedBy());

        // Free the QR mapping so the same employee can check in again
        tempQrRepo.findByQrTokenAndMappingDate(req.getTempQrToken(), today).ifPresent(m -> {
            m.setFreed(true);
            tempQrRepo.save(m);
        });

        return toDto(attendanceRepo.save(att));
    }

    /** Get all attendance records for today. */
    public List<AttendanceRecordDto> getTodayAttendance() {
        return getAttendanceByDate(LocalDate.now());
    }

    /** Get all attendance records for a specific date. */
    public List<AttendanceRecordDto> getAttendanceByDate(LocalDate date) {
        return attendanceRepo.findByAttDateOrderByCheckInAsc(date)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    /** Free all temp QR mappings and auto-checkout all checked-in employees (called at end of shift). */
    @Transactional
    public int freeAllQrs() {
        LocalDate today = LocalDate.now();
        // Auto-checkout all checked-in employees for today
        attendanceRepo.findByAttDateAndStatus(today, "CHECKED_IN").forEach(att -> {
            att.setCheckOut(LocalDateTime.now());
            att.setStatus("CHECKED_OUT");
            attendanceRepo.save(att);
        });
        // Free all QR mappings
        return tempQrRepo.freeAllForDate(today);
    }

    /** Resolve which employee holds a temp QR today (for preview before check-in). */
    public Long resolveQrToEmployee(String qrToken) {
        return tempQrRepo
                .findByQrTokenAndMappingDateAndFreedFalse(qrToken, LocalDate.now())
                .map(TempQrMapping::getEmpId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        qrToken + " is not mapped today."));
    }

    private AttendanceRecordDto toDto(Attendance a) {
        AttendanceRecordDto dto = new AttendanceRecordDto();
        dto.setAttId(a.getAttId());
        dto.setEmpId(a.getEmpId());
        dto.setTempQrToken(a.getTempQrToken());
        dto.setMachineCode(a.getMachineCode());
        dto.setAttDate(a.getAttDate());
        dto.setCheckIn(a.getCheckIn());
        dto.setCheckOut(a.getCheckOut());
        dto.setStatus(a.getStatus());
        if (a.getShiftTemplate() != null) {
            dto.setShiftName(a.getShiftTemplate().getShiftName());
        }
        employeeInfoRepo.findById(a.getEmpId()).ifPresent(e -> dto.setEmpName(e.getEmpName()));
        return dto;
    }
}
