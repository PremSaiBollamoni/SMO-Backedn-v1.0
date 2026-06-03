package com.cutm.smo.repository;

import com.cutm.smo.models.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByItemTypeAndItemIdOrderByTimestampDesc(String itemType, Long itemId);
    List<StockMovement> findByMovementTypeOrderByTimestampDesc(String movementType);
    List<StockMovement> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT sm FROM StockMovement sm WHERE sm.itemType = 'RAW_MATERIAL' AND sm.itemId = ?1 ORDER BY sm.timestamp DESC")
    List<StockMovement> findRawMaterialMovements(Long rawMaterialId);
}
