package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Physical sewing floor station (e.g. STN-01 … STN-38).
 * Each station is assigned one operation from the Operation Bulletin.
 * op_id is nullable — a station can be idle/unassigned.
 */
@Data
@Entity
@Table(name = "workstation",
        uniqueConstraints = @UniqueConstraint(name = "uk_ws_code", columnNames = "ws_code"))
public class Workstation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ws_id")
    private Long wsId;

    // Human-readable station number e.g. "STN-01"
    @Column(name = "ws_code", nullable = false, length = 20)
    private String wsCode;

    // Current operation assigned via Operation Bulletin
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "op_id")
    private Operation operation;

    // Links physical machine QR (e.g. MACHINE-001) to this station
    @Column(name = "machine_code", length = 30, unique = true)
    private String machineCode;

    // ACTIVE | INACTIVE
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";
}
