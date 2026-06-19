package com.cutm.smo.repositories;

import com.cutm.smo.models.TempQrMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TempQrMappingRepository extends JpaRepository<TempQrMapping, Long> {
    Optional<TempQrMapping> findByQrTokenAndMappingDate(String qrToken, LocalDate date);
    Optional<TempQrMapping> findByQrTokenAndMappingDateAndFreedFalse(String qrToken, LocalDate date);
    List<TempQrMapping> findByMappingDateAndFreedFalse(LocalDate date);
    boolean existsByQrTokenAndMappingDateAndFreedFalse(String qrToken, LocalDate date);

    @Modifying
    @Query("UPDATE TempQrMapping m SET m.freed = true WHERE m.mappingDate = :date AND m.freed = false")
    int freeAllForDate(LocalDate date);
}
