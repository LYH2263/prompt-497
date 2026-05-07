package com.mems.controller;

import com.mems.entity.MaintenanceRecord;
import com.mems.repository.MaintenanceRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    @Autowired
    private MaintenanceRecordRepository repository;

    @GetMapping
    public List<MaintenanceRecord> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public MaintenanceRecord create(@RequestBody MaintenanceRecord record) {
        if (record.getMaintenanceDate() == null) {
            record.setMaintenanceDate(LocalDateTime.now());
        }
        return repository.save(record);
    }
}
