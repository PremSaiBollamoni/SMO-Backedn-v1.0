package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "threads")
public class Threads {
    @Id
    @Column(name = "thread_id")
    private Long threadId;

    @Column(name = "thread_name")
    private String threadName;

    @Column(name = "thread_code")
    private String threadCode;

    @Column(name = "color_code")
    private String colorCode;

    @Column(name = "status")
    private String status;
}
