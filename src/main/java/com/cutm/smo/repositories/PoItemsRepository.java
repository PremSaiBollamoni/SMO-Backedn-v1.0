package com.cutm.smo.repositories;

import com.cutm.smo.models.PoItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PoItemsRepository extends JpaRepository<PoItems, Long> {
}