package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "grn")
public class Grn {
    @Id
    @Column(name = "grn_id")
    private Long grnId;

    @Column(name = "po_id")
    private Long poId;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "status")
    private String status;
}
