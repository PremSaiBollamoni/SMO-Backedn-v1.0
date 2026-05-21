package com.cutm.smo.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    @JsonProperty("order_id")
    private Long orderId;

    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    @JsonProperty("order_number")
    private String orderNumber;

    @Column(name = "product_id", nullable = false)
    @JsonProperty("product_id")
    private Long productId;

    @Column(name = "routing_id", nullable = false)
    @JsonProperty("routing_id")
    private Long routingId;

    @Column(name = "order_qty", nullable = false)
    @JsonProperty("order_qty")
    private Integer orderQty;

    @Column(name = "production_start_date")
    @JsonProperty("production_start_date")
    private LocalDate productionStartDate;

    @Column(name = "expected_completion_date")
    @JsonProperty("expected_completion_date")
    private LocalDate expectedCompletionDate;

    @Column(name = "customer_name")
    @JsonProperty("customer_name")
    private String customerName;

    @Column(name = "status", nullable = false, length = 50)
    @JsonProperty("status")
    private String status = "DRAFT";

    @Column(name = "created_by", nullable = false)
    @JsonProperty("created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
