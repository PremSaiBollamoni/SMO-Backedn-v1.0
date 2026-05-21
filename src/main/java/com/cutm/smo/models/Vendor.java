package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "vendor")
public class Vendor {
    @Id
    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    private String type;

    @Column(name = "status")
    private String status;
}
