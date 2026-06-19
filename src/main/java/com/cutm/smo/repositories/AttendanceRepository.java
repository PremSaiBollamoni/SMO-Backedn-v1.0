package com.cutm.smo.repositories;

import com.cutm.smo.models.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByTempQrTokenAndAttDateAndStatus(String token, LocalDate date, String status);
    List<Attendance> findByAttDateOrderByCheckInAsc(LocalDate date);
    List<Attendance> findByAttDateAndStatus(LocalDate date, String status);
    List<Attendance> findByEmpIdAndAttDateBetweenOrderByAttDateDesc(Long empId, LocalDate from, LocalDate to);
}
