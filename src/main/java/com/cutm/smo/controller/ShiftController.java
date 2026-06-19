package com.cutm.smo.controller;

import com.cutm.smo.dto.*;
import com.cutm.smo.services.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @GetMapping
    public List<ShiftTemplateDto> getAllShifts() {
        return shiftService.getAllShifts();
    }

    @GetMapping("/active")
    public List<ShiftTemplateDto> getActiveShifts() {
        return shiftService.getActiveShifts();
    }

    @PostMapping
    public ShiftTemplateDto createShift(
            @RequestBody CreateShiftTemplateRequest req,
            @RequestParam(defaultValue = "0") Long createdBy) {
        return shiftService.createShift(req, createdBy);
    }

    @PostMapping("/{templateId}/breaks")
    public ShiftTemplateDto addBreak(
            @PathVariable Long templateId,
            @RequestBody AddBreakRequest req) {
        return shiftService.addBreak(templateId, req);
    }

    @DeleteMapping("/breaks/{breakId}")
    public ResponseEntity<Void> deleteBreak(@PathVariable Long breakId) {
        shiftService.deleteBreak(breakId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{templateId}/toggle")
    public ShiftTemplateDto toggleStatus(@PathVariable Long templateId) {
        return shiftService.toggleStatus(templateId);
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteShift(@PathVariable Long templateId) {
        shiftService.deleteShift(templateId);
        return ResponseEntity.noContent().build();
    }
}
