package com.cutm.smo.services;

import com.cutm.smo.models.BreakWindow;
import com.cutm.smo.repositories.BreakWindowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Manages break windows and calculates net work duration by subtracting
 * any break time that falls within a tracking record's start/end range.
 *
 * Purely additive — does not modify any existing service or table.
 */
@Service
public class BreakWindowService {

    @Autowired
    private BreakWindowRepository breakWindowRepository;

    /** Return all active break windows ordered by start time. */
    public List<BreakWindow> getActiveBreakWindows() {
        return breakWindowRepository.findByIsActiveTrueOrderByBreakStartAsc();
    }

    /** Return all break windows (active + inactive). */
    public List<BreakWindow> getAllBreakWindows() {
        return breakWindowRepository.findAll();
    }

    /** Create a new break window. */
    public BreakWindow createBreakWindow(String name, LocalTime start, LocalTime end, Long createdBy) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Break start and end times are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Break end time must be after start time");
        }
        BreakWindow bw = new BreakWindow();
        bw.setBreakName(name != null ? name : "Break");
        bw.setBreakStart(start);
        bw.setBreakEnd(end);
        bw.setIsActive(true);
        bw.setCreatedBy(createdBy);
        return breakWindowRepository.save(bw);
    }

    /** Deactivate a break window (soft delete). */
    public void deactivateBreakWindow(Long id) {
        breakWindowRepository.findById(id).ifPresent(bw -> {
            bw.setIsActive(false);
            breakWindowRepository.save(bw);
        });
    }

    /** Hard delete a break window. */
    public void deleteBreakWindow(Long id) {
        breakWindowRepository.deleteById(id);
    }

    /**
     * Calculate net duration in seconds between start and end,
     * subtracting any overlap with active break windows.
     *
     * Works across midnight-spanning tracking records by checking
     * each calendar day in the range.
     *
     * @param start tracking start time
     * @param end   tracking end time (null = now, for in-progress)
     * @return net seconds of actual work time
     */
    public long calculateNetDurationSeconds(LocalDateTime start, LocalDateTime end) {
        if (start == null) return 0;
        LocalDateTime effectiveEnd = end != null ? end : LocalDateTime.now();
        if (!effectiveEnd.isAfter(start)) return 0;

        long totalSeconds = java.time.temporal.ChronoUnit.SECONDS.between(start, effectiveEnd);
        List<BreakWindow> breaks = getActiveBreakWindows();
        if (breaks.isEmpty()) return totalSeconds;

        long breakSeconds = 0;
        for (BreakWindow bw : breaks) {
            breakSeconds += overlapSeconds(start, effectiveEnd, bw.getBreakStart(), bw.getBreakEnd());
        }

        return Math.max(0, totalSeconds - breakSeconds);
    }

    /**
     * Calculate how many seconds of the break window [breakStart, breakEnd]
     * overlap with the tracking period [trackStart, trackEnd].
     * Handles multi-day tracking periods by iterating each calendar day.
     */
    private long overlapSeconds(LocalDateTime trackStart, LocalDateTime trackEnd,
                                LocalTime breakStart, LocalTime breakEnd) {
        long total = 0;
        java.time.LocalDate day = trackStart.toLocalDate();
        java.time.LocalDate lastDay = trackEnd.toLocalDate();

        while (!day.isAfter(lastDay)) {
            LocalDateTime bwStart = day.atTime(breakStart);
            LocalDateTime bwEnd = day.atTime(breakEnd);

            // Overlap = max(0, min(trackEnd, bwEnd) - max(trackStart, bwStart))
            LocalDateTime overlapStart = trackStart.isAfter(bwStart) ? trackStart : bwStart;
            LocalDateTime overlapEnd = trackEnd.isBefore(bwEnd) ? trackEnd : bwEnd;

            if (overlapEnd.isAfter(overlapStart)) {
                total += java.time.temporal.ChronoUnit.SECONDS.between(overlapStart, overlapEnd);
            }
            day = day.plusDays(1);
        }
        return total;
    }

    /** Format net seconds into human-readable string e.g. "1h 23m 45s" */
    public String formatDuration(long totalSeconds) {
        if (totalSeconds < 0) return "0s";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    /** Build a response map for a break window. */
    public Map<String, Object> toMap(BreakWindow bw) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", bw.getId());
        m.put("breakName", bw.getBreakName());
        m.put("breakStart", bw.getBreakStart() != null ? bw.getBreakStart().toString() : null);
        m.put("breakEnd", bw.getBreakEnd() != null ? bw.getBreakEnd().toString() : null);
        m.put("isActive", bw.getIsActive());
        m.put("createdAt", bw.getCreatedAt() != null ? bw.getCreatedAt().toString() : null);
        return m;
    }
}
