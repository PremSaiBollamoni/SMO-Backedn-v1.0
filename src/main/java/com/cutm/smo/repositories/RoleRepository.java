package com.cutm.smo.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cutm.smo.models.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleNameIgnoreCase(String roleName);

    @Query("select coalesce(max(r.roleId), 0) from Role r")
    Long findMaxRoleId();
}
