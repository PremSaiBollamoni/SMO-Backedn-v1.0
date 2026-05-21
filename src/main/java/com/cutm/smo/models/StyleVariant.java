package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "style_variant")
public class StyleVariant {
    @Id
    @Column(name = "style_variant_id")
    private Long styleVariantId;

    @Column(name = "style_id")
    private Long styleId;

    @Column(name = "button_id")
    private Long buttonId;

    @Column(name = "thread_id")
    private Long threadId;

    @Column(name = "gtg_id")
    private String gtgId;

    @Column(name = "size")
    private String size;

    @Column(name = "sleeve_type")
    private String sleeveType;

    @Column(name = "color")
    private String color;

    @Column(name = "consumption_per_shirt")
    private BigDecimal consumptionPerShirt;

    @Column(name = "no_of_shirts_target")
    private Integer noOfShirtsTarget;

    @Column(name = "status")
    private String status;

    // Additional fields copied from Style
    @Column(name = "main_label")
    private String mainLabel;

    @Column(name = "branding_label")
    private String brandingLabel;

    @Column(name = "concept")
    private String concept;

    @Column(name = "style_no")
    private String styleNo;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "pattern_image", columnDefinition = "TEXT")
    private String patternImage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
