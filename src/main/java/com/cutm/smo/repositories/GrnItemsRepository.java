package com.cutm.smo.repositories;

import com.cutm.smo.models.GrnItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrnItemsRepository extends JpaRepository<GrnItems, Long> {
}