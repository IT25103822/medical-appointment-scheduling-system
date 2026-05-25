package com.hospital.controllers;

import com.hospital.models.MedicalRecord;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/ehr")
@CrossOrigin(origins = "*")
public class EHRController {

    private static final String RECORDS_FILE = "data/medical_records.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // CREATE – Add new patient encounter
    @PostMapping("/create")
    public Map<String, Object> createRecord(@RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            String recordId = "REC" + (10000 + new Random().nextInt(90000));
            String patientId = body.getOrDefault("patientId", "");
            String providerId = body.getOrDefault("providerId", "");
            String appointmentId = body.getOrDefault("appointmentId", "");
            String notes = body.getOrDefault("notes", "").replace("|", "-");
            String labResults = body.getOrDefault("labResults", "N/A").replace("|", "-");
            String createdAt = LocalDateTime.now().format(FORMATTER);

            if (patientId.isEmpty() || notes.isEmpty()) {
                res.put("success", false);
                res.put("message", "Patient ID and notes are required.");
                return res;
            }

            String line = String.join("|", recordId, patientId, providerId,
                    appointmentId, notes, labResults, createdAt, "false");
            Files.writeString(Path.of(RECORDS_FILE), line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            res.put("success", true);
            res.put("recordId", recordId);
            res.put("message", "Medical record created successfully.");
        } catch (IOException e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    // READ – Retrieve medical records for a patient
    @GetMapping("/{patientId}")
    public Map<String, Object> getRecords(@PathVariable("patientId") String patientId) {
        Map<String, Object> res = new HashMap<>();
        List<Map<String, String>> records = new ArrayList<>();
        try {
            File file = new File(RECORDS_FILE);
            if (file.exists()) {
                for (String line : Files.readAllLines(Path.of(RECORDS_FILE))) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split("\\|", -1);
                    if (parts.length >= 8 && parts[1].equals(patientId) && parts[7].equals("false")) {
                        Map<String, String> record = new LinkedHashMap<>();
                        record.put("recordId", parts[0]);
                        record.put("patientId", parts[1]);
                        record.put("providerId", parts[2]);
                        record.put("appointmentId", parts[3]);
                        record.put("notes", parts[4]);
                        record.put("labResults", parts[5]);
                        record.put("createdAt", parts[6]);
                        records.add(record);
                    }
                }
            }
            res.put("success", true);
            res.put("records", records);
        } catch (IOException e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    // READ ALL – Get all active medical records (Admin)
    @GetMapping("/all")
    public Map<String, Object> getAllRecords() {
        Map<String, Object> res = new HashMap<>();
        List<Map<String, String>> records = new ArrayList<>();
        try {
            File file = new File(RECORDS_FILE);
            if (file.exists()) {
                for (String line : Files.readAllLines(Path.of(RECORDS_FILE))) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split("\\|", -1);
                    if (parts.length >= 8) {
                        Map<String, String> record = new LinkedHashMap<>();
                        record.put("recordId", parts[0]);
                        record.put("patientId", parts[1]);
                        record.put("providerId", parts[2]);
                        record.put("appointmentId", parts[3]);
                        record.put("notes", parts[4]);
                        record.put("labResults", parts[5]);
                        record.put("createdAt", parts[6]);
                        record.put("archived", parts[7]);
                        records.add(record);
                    }
                }
            }
            res.put("success", true);
            res.put("records", records);
        } catch (IOException e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    // UPDATE – Append clinical notes or lab results
    @PutMapping("/update/{recordId}")
    public Map<String, Object> updateRecord(@PathVariable("recordId") String recordId,
                                             @RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            File file = new File(RECORDS_FILE);
            if (!file.exists()) {
                res.put("success", false);
                res.put("message", "No records found.");
                return res;
            }
            List<String> lines = Files.readAllLines(Path.of(RECORDS_FILE));
            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).isBlank()) continue;
                String[] parts = lines.get(i).split("\\|", -1);
                if (parts.length >= 8 && parts[0].equals(recordId)) {
                    String appendNote = body.getOrDefault("notes", "").replace("|", "-");
                    String appendLab = body.getOrDefault("labResults", "").replace("|", "-");
                    if (!appendNote.isEmpty()) {
                        parts[4] = parts[4] + " | " + appendNote;
                    }
                    if (!appendLab.isEmpty()) {
                        parts[5] = parts[5] + " | " + appendLab;
                    }
                    lines.set(i, String.join("|", parts));
                    found = true;
                    break;
                }
            }
            if (!found) {
                res.put("success", false);
                res.put("message", "Record not found: " + recordId);
                return res;
            }
            Files.write(Path.of(RECORDS_FILE), lines);
            res.put("success", true);
            res.put("message", "Record updated successfully.");
        } catch (IOException e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    // DELETE – Archive a completed visit
    @DeleteMapping("/archive/{recordId}")
    public Map<String, Object> archiveRecord(@PathVariable("recordId") String recordId) {
        Map<String, Object> res = new HashMap<>();
        try {
            File file = new File(RECORDS_FILE);
            if (!file.exists()) {
                res.put("success", false);
                res.put("message", "No records found.");
                return res;
            }
            List<String> lines = Files.readAllLines(Path.of(RECORDS_FILE));
            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).isBlank()) continue;
                String[] parts = lines.get(i).split("\\|", -1);
                if (parts.length >= 8 && parts[0].equals(recordId)) {
                    parts[7] = "true";
                    lines.set(i, String.join("|", parts));
                    found = true;
                    break;
                }
            }
            if (!found) {
                res.put("success", false);
                res.put("message", "Record not found: " + recordId);
                return res;
            }
            Files.write(Path.of(RECORDS_FILE), lines);
            res.put("success", true);
            res.put("message", "Visit archived successfully.");
        } catch (IOException e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }
}
