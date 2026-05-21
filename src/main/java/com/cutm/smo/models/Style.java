package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "style")
public class Style {
    @Id
    @Column(name = "style_id")
    private Long styleId;

    @Column(name = "style_no")
    private String styleNo;

    @Column(name = "concept")
    private String concept;

    @Column(name = "main_label")
    private String mainLabel;

    @Column(name = "branding_label")
    private String brandingLabel;

    @Column(name = "pattern_image", columnDefinition = "TEXT")
    private String patternImage;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
