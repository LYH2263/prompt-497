package com.mems.controller;

import com.mems.entity.Equipment;
import com.mems.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportEquipment() throws IOException {
        List<Equipment> equipmentList = equipmentRepository.findAll();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        // CSV Header
        writer.println("ID,名称,型号,位置,状态,安装日期");

        // CSV Content
        for (Equipment eq : equipmentList) {
            writer.printf("%d,%s,%s,%s,%s,%s%n",
                    eq.getId(),
                    escape(eq.getName()),
                    escape(eq.getType()),
                    escape(eq.getLocation()),
                    eq.getStatus(),
                    eq.getInstallationDate()
            );
        }
        writer.flush();
        writer.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=equipment_report.csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(baos.toByteArray());
    }

    private String escape(String data) {
        if (data == null) return "";
        return data.replace(",", " ");
    }
}
