package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stockmovement")
public class StockMovement {
    @Id
    @Column(name = "movement_id")
    private Long movementId;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "type")
    private String type;

    @Column(name = "qty")
    private Integer qty;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}
