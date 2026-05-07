package com.mems.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "equipment")
public class Equipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String status;
    private String location;
    private String type;

    @Column(name = "last_maintenance")
    private LocalDateTime lastMaintenance;

    @Column(name = "installation_date")
    private LocalDate installationDate;
}
