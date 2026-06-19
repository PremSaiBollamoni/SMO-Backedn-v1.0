package com.cutm.smo.repositories;

import com.cutm.smo.models.LoginAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAuditLogRepository extends JpaRepository<LoginAuditLog, Long> {

    // Find all login attempts for an employee
    List<LoginAuditLog> findByEmpIdOrderByAttemptedAtDesc(Long empId);

    // Find login attempts within a time range
    @Query("SELECT l FROM LoginAuditLog l WHERE l.empId = :empId AND l.attemptedAt BETWEEN :start AND :end")
    List<LoginAuditLog> findByEmpIdAndDateRange(
            @Param("empId") Long empId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Find failed login attempts
    @Query("SELECT l FROM LoginAuditLog l WHERE l.empId = :empId AND l.loginStatus IN ('FAILED', 'LOCKED') ORDER BY l.attemptedAt DESC")
    List<LoginAuditLog> findFailedAttempts(@Param("empId") Long empId);

    // Count failed attempts in last N minutes
    @Query("SELECT COUNT(l) FROM LoginAuditLog l WHERE l.empId = :empId AND l.loginStatus = 'FAILED' AND l.attemptedAt >= :since")
    long countFailedAttemptsRecently(@Param("empId") Long empId, @Param("since") LocalDateTime since);

    // Find all login attempts from an IP address
    List<LoginAuditLog> findByIpAddressOrderByAttemptedAtDesc(String ipAddress);
}
