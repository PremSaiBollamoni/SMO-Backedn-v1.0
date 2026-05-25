package com.cutm.smo.repositories;

import com.cutm.smo.models.Bom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BomRepository extends JpaRepository<Bom, Long> {
}