package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "label")
public class Label {
    
    @Id
    @Column(name = "label_id")
    private Long labelId;
    
    @Column(name = "label_code")
    private String labelCode;
    
    @Column(name = "label_name")
    private String labelName;
    
    @Column(name = "label_type")
    private String labelType;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
