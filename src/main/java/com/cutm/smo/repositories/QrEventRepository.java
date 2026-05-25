package com.cutm.smo.repositories;

import com.cutm.smo.models.QrEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QrEventRepository extends JpaRepository<QrEvent, Long> {
}