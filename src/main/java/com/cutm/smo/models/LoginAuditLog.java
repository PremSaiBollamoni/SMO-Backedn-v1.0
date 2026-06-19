package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "login_audit")
public class LoginAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_audit_id")
    private Long loginAuditId;

    @Column(name = "emp_id", nullable = false)
    private Long empId;

    @Column(name = "login_status", nullable = false, length = 20)
    private String loginStatus;  // SUCCESS, FAILED, LOCKED

    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @Column(name = "attempt_count")
    private Integer attemptCount;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    // Constructor for successful login
    public LoginAuditLog(Long empId, String ipAddress, String userAgent) {
        this.empId = empId;
        this.loginStatus = "SUCCESS";
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.attemptedAt = LocalDateTime.now();
    }

    // Constructor for failed login
    public LoginAuditLog(Long empId, String loginStatus, String failureReason,
                        Integer attemptCount, String ipAddress, String userAgent) {
        this.empId = empId;
        this.loginStatus = loginStatus;
        this.failureReason = failureReason;
        this.attemptCount = attemptCount;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.attemptedAt = LocalDateTime.now();
    }
}
