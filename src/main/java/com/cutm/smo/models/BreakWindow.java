package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Represents a recurring daily break window (e.g. lunch 13:00-14:00).
 * When calculating actual work duration, any overlap between a tracking
 * record's start/end and an active break window is subtracted.
 * This table is purely additive — no existing tables are modified.
 */
@Data
@Entity
@Table(name = "break_windows")
public class BreakWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "break_name", nullable = false)
    private String breakName;

    @Column(name = "break_start", nullable = false)
    private LocalTime breakStart;

    @Column(name = "break_end", nullable = false)
    private LocalTime breakEnd;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
    }
}
