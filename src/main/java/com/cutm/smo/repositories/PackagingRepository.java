package com.cutm.smo.repositories;

import com.cutm.smo.models.Packaging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PackagingRepository extends JpaRepository<Packaging, Long> {
}