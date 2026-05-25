package com.cutm.smo.repositories;

import com.cutm.smo.models.Bundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BundleRepository extends JpaRepository<Bundle, Long> {
}