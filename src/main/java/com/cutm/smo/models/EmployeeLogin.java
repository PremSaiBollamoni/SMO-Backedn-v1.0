package com.cutm.smo.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(exclude = "employee")
@ToString(exclude = "employee")
@Entity
@Table(name = "login")
public class EmployeeLogin {

    @Id
    @Column(name = "emp_id", nullable = false)
    private Long empId;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id", insertable = false, updatable = false)
    private EmployeeInfo employee;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "password_hash_version", nullable = false)
    private Integer passwordHashVersion = 2;  // 1=plain, 2=bcrypt

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_login_attempt")
    private LocalDateTime lastLoginAttempt;

    @Column(name = "locked")
    private Boolean locked = false;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @PrePersist
    void prePersist() {
        if (passwordChangedAt == null) {
            passwordChangedAt = LocalDateTime.now();
        }
        if (passwordHashVersion == null) {
            passwordHashVersion = 2;  // Default to BCrypt
        }
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
        if (locked == null) {
            locked = false;
        }
    }
}
