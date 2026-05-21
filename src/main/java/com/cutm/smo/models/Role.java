package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "role",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_role_role_name", columnNames = "role_name")
        }
)
public class Role {
    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "activities", length = 500)
    private String activity;

    @Column(name = "status", nullable = false, length = 50)
    private String status;
}
