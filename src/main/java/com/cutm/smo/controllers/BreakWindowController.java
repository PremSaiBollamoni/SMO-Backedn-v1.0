package com.cutm.smo.controllers;

import com.cutm.smo.models.BreakWindow;
import com.cutm.smo.services.BreakWindowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST endpoints for managing break windows.
 * All endpoints are under /api/supervisor/break-windows.
 * Purely additive — no existing controllers modified.
 */
@RestController
@RequestMapping("/api/supervisor/break-windows")
@CrossOrigin(origins = "*")
public class BreakWindowController {

    @Autowired
    private BreakWindowService breakWindowService;

    /** GET /api/supervisor/break-windows — list all break windows */
    @GetMapping
    public List<Map<String, Object>> getAllBreakWindows() {
        return breakWindowService.getAllBreakWindows().stream()
                .map(breakWindowService::toMap)
                .collect(Collectors.toList());
    }

    /** GET /api/supervisor/break-windows/active — list active break windows only */
    @GetMapping("/active")
    public List<Map<String, Object>> getActiveBreakWindows() {
        return breakWindowService.getActiveBreakWindows().stream()
                .map(breakWindowService::toMap)
                .collect(Collectors.toList());
    }

    /**
     * POST /api/supervisor/break-windows — create a new break window
     * Body: { "breakName": "Lunch", "breakStart": "13:00", "breakEnd": "14:00", "createdBy": 1001 }
     */
    @PostMapping
    public Map<String, Object> createBreakWindow(@RequestBody Map<String, Object> body) {
        String name = (String) body.getOrDefault("breakName", "Break");
        String startStr = (String) body.get("breakStart");
        String endStr = (String) body.get("breakEnd");
        Long createdBy = body.get("createdBy") != null
                ? Long.parseLong(body.get("createdBy").toString()) : null;

        if (startStr == null || endStr == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "breakStart and breakEnd are required (format: HH:mm)");
        }

        try {
            LocalTime start = LocalTime.parse(startStr);
            LocalTime end = LocalTime.parse(endStr);
            BreakWindow bw = breakWindowService.createBreakWindow(name, start, end, createdBy);
            return breakWindowService.toMap(bw);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid time format. Use HH:mm (e.g. 13:00)");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * DELETE /api/supervisor/break-windows/{id} — delete a break window
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteBreakWindow(@PathVariable Long id) {
        breakWindowService.deleteBreakWindow(id);
        return Map.of("success", true, "message", "Break window deleted");
    }

    /**
     * PATCH /api/supervisor/break-windows/{id}/deactivate — soft deactivate
     */
    @PatchMapping("/{id}/deactivate")
    public Map<String, Object> deactivateBreakWindow(@PathVariable Long id) {
        breakWindowService.deactivateBreakWindow(id);
        return Map.of("success", true, "message", "Break window deactivated");
    }
}
