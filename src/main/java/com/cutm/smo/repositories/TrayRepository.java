package com.cutm.smo.repositories;

import com.cutm.smo.models.Tray;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrayRepository extends JpaRepository<Tray, Long> {
    Optional<Tray> findByTrayNumber(String trayNumber);
}
