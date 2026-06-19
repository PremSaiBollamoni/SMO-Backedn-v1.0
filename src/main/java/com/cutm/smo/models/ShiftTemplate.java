package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "shift_template")
public class ShiftTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "shift_name", nullable = false, length = 100)
    private String shiftName;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // ACTIVE | INACTIVE
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "shiftTemplate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ShiftBreak> breaks;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
