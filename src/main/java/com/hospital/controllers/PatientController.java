package com.hospital.controllers;

import com.hospital.models.Patient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
@CrossOrigin
public class PatientController {

    private static final String FILE_PATH = "data/patients.txt";

    private String getCurrentDateTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
        return LocalDateTime.now().format(dtf);
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerPatient(@RequestBody Patient patient) {
        try {
            String newId = "P" + (int) (Math.random() * 10000);
            String initialHistory = getCurrentDateTime() + " - " + sanitize(patient.getMedicalHistory());
            String record = newId + "|" + sanitize(patient.getName()) + "|" + sanitize(patient.getAge()) + "|" +
                    sanitize(patient.getAddress()) + "|" + sanitize(patient.getMobile()) + "|" +
                    initialHistory + System.lineSeparator();
            Files.write(Paths.get(FILE_PATH), record.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return ResponseEntity.ok(newId);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error");
        }
    }

    @GetMapping("/all")
    public List<Patient> getAllPatients() {
        List<Patient> patients = new ArrayList<>();
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                return patients;
            }

            for (String line : Files.readAllLines(Paths.get(FILE_PATH))) {
                Patient patient = parsePatient(line);
                if (patient != null) {
                    patients.add(patient);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return patients;
    }

    @GetMapping("/login/{id}")
    public ResponseEntity<?> loginPatient(@PathVariable("id") String id) {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) return ResponseEntity.status(404).body("NOT_FOUND");
            String normalizedId = normalizeId(id);

            for (String line : Files.readAllLines(Paths.get(FILE_PATH))) {
                Patient patient = parsePatient(line);
                if (patient != null && normalizeId(patient.getId()).equals(normalizedId)) {
                    return ResponseEntity.ok(patient);
                }
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body("ERROR");
        }
        return ResponseEntity.status(404).body("NOT_FOUND");
    }

    @GetMapping("/recover")
    public ResponseEntity<?> recoverPatientId(@RequestParam("name") String name, @RequestParam("mobile") String mobile) {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) return ResponseEntity.status(404).body("No database");
            String normalizedName = sanitize(name).toLowerCase();
            String normalizedMobile = sanitize(mobile);

            for (String line : Files.readAllLines(Paths.get(FILE_PATH))) {
                String[] data = line.split("\\|", -1);
                if (data.length >= 6 &&
                        sanitize(data[1]).toLowerCase().contains(normalizedName) &&
                        sanitize(data[4]).equals(normalizedMobile)) {
                    return ResponseEntity.ok(data[0]);
                }
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body("ERROR");
        }
        return ResponseEntity.status(404).body("NOT_FOUND");
    }

    @PutMapping("/update/{id}")
    public String updatePatient(@PathVariable("id") String id, @RequestBody Patient patient) {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) return "ERROR: No Data";

            List<String> updatedLines = new ArrayList<>();
            boolean found = false;

            for (String line : Files.readAllLines(Paths.get(FILE_PATH))) {
                if (line.startsWith(id + "|")) {
                    updatedLines.add(id + "|" + sanitize(patient.getName()) + "|" + sanitize(patient.getAge()) + "|" +
                            sanitize(patient.getAddress()) + "|" + sanitize(patient.getMobile()) + "|" +
                            sanitize(patient.getMedicalHistory()));
                    found = true;
                } else {
                    updatedLines.add(line);
                }
            }

            Files.write(Paths.get(FILE_PATH), updatedLines, StandardOpenOption.TRUNCATE_EXISTING);
            return found ? "SUCCESS" : "NOT_FOUND";
        } catch (IOException e) {
            return "ERROR";
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deletePatient(@PathVariable("id") String id) {
        try {
            List<String> updatedLines = new ArrayList<>();
            boolean found = false;

            for (String line : Files.readAllLines(Paths.get(FILE_PATH))) {
                String[] data = line.split("\\|", -1);
                if (!data[0].equals(id)) {
                    updatedLines.add(line);
                } else {
                    found = true;
                }
            }

            if (found) {
                Files.write(Paths.get(FILE_PATH), updatedLines);
                return ResponseEntity.ok("SUCCESS");
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body("ERROR");
        }
        return ResponseEntity.status(404).body("NOT_FOUND");
    }

    private Patient parsePatient(String line) {
        if (line == null || line.isBlank()) return null;
        String[] data = line.split("\\|", -1);
        if (data.length < 6) return null;

        Patient patient = new Patient();
        patient.setId(data[0]);
        patient.setName(data[1]);
        patient.setAge(data[2]);
        patient.setAddress(data[3]);
        patient.setMobile(data[4]);
        patient.setMedicalHistory(data[5]);
        return patient;
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace("|", "/").trim();
    }

    private String normalizeId(String value) {
        return sanitize(value).toUpperCase().replaceAll("\\s+", "");
    }
}
