package com.mems.controller;

import com.mems.entity.Equipment;
import com.mems.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @GetMapping
    public List<Equipment> getAllEquipment() {
        return equipmentRepository.findAll();
    }

    @PostMapping
    public Equipment createEquipment(@RequestBody Equipment equipment) {
        if (equipment.getLastMaintenance() == null) {
            equipment.setLastMaintenance(LocalDateTime.now());
        }
        return equipmentRepository.save(equipment);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long total = equipmentRepository.count();
        // Assuming '正常运行' means Active
        long active = equipmentRepository.countByStatus("正常运行");
        long warning = equipmentRepository.countByStatus("故障报警");
        long maintenance = equipmentRepository.countByStatus("维护中");

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("active", active);
        stats.put("warning", warning);
        stats.put("maintenance", maintenance);

        return ResponseEntity.ok(stats);
    }
}
