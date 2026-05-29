package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Many-to-many mapping between employees and roles.
 * Additive — does not modify the existing employee.role_id FK.
 * The existing role_id on employee stays as the "primary" role for
 * backward compatibility with all existing code.
 */
@Data
@Entity
@Table(name = "employee_roles",
       uniqueConstraints = @UniqueConstraint(columnNames = {"emp_id", "role_id"}))
public class EmployeeRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "emp_id", nullable = false)
    private Long empId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;
}
