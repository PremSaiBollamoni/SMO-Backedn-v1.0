package com.cutm.smo.repository;

import com.cutm.smo.models.DailyStockLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyStockLedgerRepository extends JpaRepository<DailyStockLedger, Long> {
    List<DailyStockLedger> findByLedgerDate(LocalDate ledgerDate);
    List<DailyStockLedger> findByLedgerDateBetween(LocalDate startDate, LocalDate endDate);
    Optional<DailyStockLedger> findByLedgerDateAndLocationTypeAndLocationIdAndItemType(
            LocalDate ledgerDate, String locationType, Long locationId, String itemType);
}
