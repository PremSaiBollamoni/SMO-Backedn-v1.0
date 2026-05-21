package com.cutm.smo.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(exclude = "employee")
@ToString(exclude = "employee")
@Entity
@Table(name = "login")
public class EmployeeLogin {

    @Id
    @Column(name = "emp_id", nullable = false)
    private Long empId;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id", insertable = false, updatable = false)
    private EmployeeInfo employee;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "status", nullable = false, length = 50)
    private String status;
}
