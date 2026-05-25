package com.hospital.controllers;

import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

@RestController
@RequestMapping("/api/providers")
@CrossOrigin
public class ProviderController {

    private static final String FILE_PATH = "data/providers.txt";

    @GetMapping("/all")
    public List<Map<String, String>> getAllProviders() {
        List<Map<String, String>> list = new ArrayList<>();
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) return list;

            for (String line : Files.readAllLines(Paths.get(FILE_PATH))) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split("\\|", -1);
                if (data.length >= 3) {
                    Map<String, String> map = new HashMap<>();
                    map.put("id", data[0]);
                    map.put("name", data[1]);
                    map.put("specialization", data[2]);
                    map.put("fee", data.length > 3 && !data[3].isEmpty() ? data[3] : "2000");
                    map.put("photo", data.length > 4 ? data[4] : "");
                    list.add(map);
                }
            }
        } catch (IOException ignored) {
        }
        return list;
    }

    @PostMapping("/add")
    public Map<String, Object> addProvider(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String id = "DOC" + (int) (Math.random() * 10000);
            String fee = payload.getOrDefault("fee", "2000");
            String photo = payload.getOrDefault("photo", "").replaceAll("[\\n\\r]", "");

            String record = id + "|" + payload.get("name") + "|" + payload.get("specialization") + "|" + fee + "|" + photo + System.lineSeparator();
            Files.write(Paths.get(FILE_PATH), record.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            response.put("success", true);
            response.put("id", id);
            response.put("message", "Provider added successfully.");
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Failed to add provider.");
        }
        return response;
    }

    @PutMapping("/update/{id}")
    public Map<String, Object> updateProvider(@PathVariable("id") String id, @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> updatedLines = new ArrayList<>();
            boolean found = false;

            for (String line : Files.readAllLines(Paths.get(FILE_PATH))) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split("\\|", -1);
                if (data[0].equals(id)) {
                    String name = payload.getOrDefault("name", data.length > 1 ? data[1] : "");
                    String specialization = payload.getOrDefault("specialization", data.length > 2 ? data[2] : "");
                    String fee = payload.getOrDefault("fee", data.length > 3 && !data[3].isEmpty() ? data[3] : "2000");
                    String photo = payload.getOrDefault("photo", data.length > 4 ? data[4] : "").replaceAll("[\\n\\r]", "");
                    updatedLines.add(id + "|" + name + "|" + specialization + "|" + fee + "|" + photo);
                    found = true;
                } else {
                    updatedLines.add(line);
                }
            }

            Files.write(Paths.get(FILE_PATH), updatedLines, StandardOpenOption.TRUNCATE_EXISTING);
            response.put("success", found);
            response.put("message", found ? "Provider updated successfully." : "Provider not found.");
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Failed to update provider.");
        }
        return response;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteProvider(@PathVariable("id") String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> updatedLines = new ArrayList<>();
            boolean found = false;

            for (String line : Files.readAllLines(Paths.get(FILE_PATH))) {
                if (line.trim().isEmpty()) continue;
                if (!line.startsWith(id + "|")) {
                    updatedLines.add(line);
                } else {
                    found = true;
                }
            }

            Files.write(Paths.get(FILE_PATH), updatedLines, StandardOpenOption.TRUNCATE_EXISTING);
            response.put("success", found);
            response.put("message", found ? "Provider deleted successfully." : "Provider not found.");
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Failed to delete provider.");
        }
        return response;
    }
}
