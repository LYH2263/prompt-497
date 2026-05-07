package com.mems.controller;

import com.mems.repository.EquipmentRepository;
import com.mems.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        long totalEquipment = equipmentRepository.count();
        long activeEquipment = equipmentRepository.countByStatus("正常运行");
        long warningEquipment = equipmentRepository.countByStatus("故障报警");
        long activeUsers = userRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEquipment", totalEquipment);
        stats.put("activeEquipment", activeEquipment);
        stats.put("warningEquipment", warningEquipment);
        stats.put("activeUsers", activeUsers);

        // Mocking uptime for now as it requires complex logic
        stats.put("uptime", "99.8%");

        return ResponseEntity.ok(stats);
    }
}
