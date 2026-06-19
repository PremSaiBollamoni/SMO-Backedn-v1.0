package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tray")
public class Tray {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tray_id")
    private Long trayId;

    @Column(name = "tray_number", unique = true, nullable = false)
    private String trayNumber;

    @Column(name = "status", nullable = false)
    private String status; // FREE or ASSIGNED

    @Column(name = "assigned_to")
    private Long assignedTo; // emp_id

    @Column(name = "assigned_by")
    private Long assignedBy; // supervisor emp_id

    @Column(name = "unassigned_by")
    private Long unassignedBy; // supervisor emp_id

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "unassigned_at")
    private LocalDateTime unassignedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
