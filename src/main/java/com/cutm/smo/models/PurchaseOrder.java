package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "purchaseorder")
public class PurchaseOrder {
    @Id
    @Column(name = "po_id")
    private Long poId;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "status")
    private String status;
}
