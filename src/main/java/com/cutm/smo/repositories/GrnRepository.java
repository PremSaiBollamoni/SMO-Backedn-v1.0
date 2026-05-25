package com.cutm.smo.repositories;

import com.cutm.smo.models.Grn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrnRepository extends JpaRepository<Grn, Long> {
}