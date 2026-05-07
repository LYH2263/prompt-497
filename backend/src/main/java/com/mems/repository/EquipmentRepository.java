package com.mems.repository;

import com.mems.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    long countByStatus(String status);
}
