package com.mems.repository;

import com.mems.entity.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {
}
