package com.cutm.smo.repositories;

import com.cutm.smo.models.TempActiveAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface TempActiveAssignmentRepository extends JpaRepository<TempActiveAssignment, Long> {
    
    /**
     * Find active assignment by machine QR, tray QR, and employee ID combination
     */
    @Query("SELECT t FROM TempActiveAssignment t WHERE t.machineQr = :machineQr AND t.trayQr = :trayQr AND t.empId = :empId")
    Optional<TempActiveAssignment> findByMachineQrAndTrayQrAndEmpId(
        @Param("machineQr") String machineQr,
        @Param("trayQr") String trayQr,
        @Param("empId") Long empId
    );

    /**
     * Find active assignment by employee ID and status (for checking double-booking)
     */
    @Query("SELECT t FROM TempActiveAssignment t WHERE t.empId = :empId AND t.status = :status")
    Optional<TempActiveAssignment> findByEmpIdAndStatus(
        @Param("empId") Long empId,
        @Param("status") String status
    );

    /**
     * Find active assignment by machine QR and status (for checking machine availability)
     */
    @Query("SELECT t FROM TempActiveAssignment t WHERE t.machineQr = :machineQr AND t.status = :status")
    Optional<TempActiveAssignment> findByMachineQrAndStatus(
        @Param("machineQr") String machineQr,
        @Param("status") String status
    );

    /**
     * Find active assignment by tray QR and status (for checking tray availability)
     */
    @Query("SELECT t FROM TempActiveAssignment t WHERE t.trayQr = :trayQr AND t.status = :status")
    Optional<TempActiveAssignment> findByTrayQrAndStatus(
        @Param("trayQr") String trayQr,
        @Param("status") String status
    );

    /**
     * Find maximum temp ID for generating new IDs if needed
     */
    @Query("SELECT MAX(t.tempId) FROM TempActiveAssignment t")
    Long findMaxTempId();

    /**
     * Find any active assignment by machine QR, tray QR, and status (for team assignments)
     * NEW: Support for checking team assignments (multiple employees on same machine+tray)
     */
    @Query("SELECT t FROM TempActiveAssignment t WHERE t.machineQr = :machineQr AND t.trayQr = :trayQr AND t.status = :status")
    List<TempActiveAssignment> findByMachineQrAndTrayQrAndStatus(
        @Param("machineQr") String machineQr,
        @Param("trayQr") String trayQr,
        @Param("status") String status
    );

    /**
     * Count distinct active jobs at a specific routing + operation node (metric 2)
     * Counts active assignments for the operation
     */
    @Query("SELECT COUNT(DISTINCT taa.tempId) FROM TempActiveAssignment taa WHERE LOWER(taa.status) = 'active' AND FUNCTION('DATE', taa.startTime) = CURRENT_DATE")
    int countActiveJobsAtNode(@Param("routingId") Long routingId, @Param("operationId") Long operationId);

    /**
     * Count active operators per operation by joining temp_active_assignments with bin table
     * Returns pairs of [operation_id, count]
     */
    @Query(value = "SELECT b.current_operation_id AS operation_id, COUNT(DISTINCT t.temp_id) AS active_count " +
                   "FROM temp_active_assignments t " +
                   "JOIN bin b ON b.qr_code = t.tray_qr " +
                   "WHERE t.status IN ('active', 'assigned') AND b.current_operation_id IS NOT NULL " +
                   "GROUP BY b.current_operation_id", nativeQuery = true)
    List<Object[]> countActiveOperatorsByOperation();
}