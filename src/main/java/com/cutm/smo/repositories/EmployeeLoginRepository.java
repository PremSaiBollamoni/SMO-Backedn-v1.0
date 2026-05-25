package com.cutm.smo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cutm.smo.models.EmployeeLogin;

public interface EmployeeLoginRepository extends JpaRepository<EmployeeLogin, Long> {
}
