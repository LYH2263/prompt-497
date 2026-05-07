package com.mems.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "maintenance_record")
public class MaintenanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id")
    private Long equipmentId;

    private String technician;
    private String description;

    @Column(name = "maintenance_date")
    private LocalDateTime maintenanceDate;

    private String status;
}
