package com.cutm.smo.repository;

import com.cutm.smo.models.InventoryStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {
    List<InventoryStock> findByLocationType(String locationType);
    List<InventoryStock> findByLocationTypeAndLocationId(String locationType, Long locationId);
}
