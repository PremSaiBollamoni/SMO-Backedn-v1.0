package com.cutm.smo.repository;

import com.cutm.smo.models.RawMaterialInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RawMaterialInventoryRepository extends JpaRepository<RawMaterialInventory, Long> {
    Optional<RawMaterialInventory> findByMaterialCode(String materialCode);
    List<RawMaterialInventory> findByMaterialType(String materialType);
    
    @Query("SELECT r FROM RawMaterialInventory r WHERE r.currentStock <= r.minStockLevel")
    List<RawMaterialInventory> findLowStockMaterials();
    
    @Query("SELECT r FROM RawMaterialInventory r WHERE r.currentStock <= r.reorderLevel")
    List<RawMaterialInventory> findReorderMaterials();
}
