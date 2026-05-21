package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "buttons")
public class Buttons {
    @Id
    @Column(name = "button_id")
    private Long buttonId;

    @Column(name = "button_name")
    private String buttonName;

    @Column(name = "button_code")
    private String buttonCode;

    @Column(name = "status")
    private String status;
}
