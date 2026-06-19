package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One attendance record = one employee checked in at one machine for one day.
 * temp_qr_token: e.g. EMP-TEMP-001 (physical card handed to employee daily)
 * machine_code:  e.g. MACHINE-001 (QR fixed on the machine)
 * status: CHECKED_IN | CHECKED_OUT
 */
@Data
@Entity
@Table(name = "attendance",
        uniqueConstraints = @UniqueConstraint(name = "uk_att_qr_date",
                columnNames = {"temp_qr_token", "att_date"}))
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "att_id")
    private Long attId;

    @Column(name = "emp_id", nullable = false)
    private Long empId;

    @Column(name = "temp_qr_token", nullable = false, length = 50)
    private String tempQrToken;

    @Column(name = "machine_code", nullable = false, length = 50)
    private String machineCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id")
    private ShiftTemplate shiftTemplate;

    @Column(name = "att_date", nullable = false)
    private LocalDate attDate;

    @Column(name = "check_in", nullable = false)
    private LocalDateTime checkIn;

    @Column(name = "check_out")
    private LocalDateTime checkOut;

    // CHECKED_IN | CHECKED_OUT
    @Column(name = "status", nullable = false, length = 20)
    private String status = "CHECKED_IN";

    @Column(name = "marked_by")
    private Long markedBy;
}
