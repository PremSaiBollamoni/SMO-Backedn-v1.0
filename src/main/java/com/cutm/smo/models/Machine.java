package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "machine")
public class Machine {
    @Id
    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    private String type;

    @Column(name = "status")
    private String status;
}
