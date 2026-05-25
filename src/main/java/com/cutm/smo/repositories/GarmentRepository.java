package com.cutm.smo.repositories;

import com.cutm.smo.models.Garment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GarmentRepository extends JpaRepository<Garment, Long> {
    
    /**
     * Find garment by QR code
     */
    Optional<Garment> findByQrCode(String qrCode);
    
    /**
     * Find maximum garment ID for generating new IDs
     */
    @Query("SELECT MAX(g.garmentId) FROM Garment g")
    Long findMaxGarmentId();
}