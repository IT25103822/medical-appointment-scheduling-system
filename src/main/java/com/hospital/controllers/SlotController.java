package com.hospital.controllers;

import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/api/slots")
@CrossOrigin("*")
public class SlotController {

    private static final String SLOTS_FILE = "data/slots.txt";

    @PostMapping("/add")
    public Map<String, Object> addSlot(@RequestBody Map<String, String> data) {
        Map<String, Object> response = new HashMap<>();
        String doctorId = data.getOrDefault("doctorId", data.getOrDefault("docId", "")).trim();
        String date = data.getOrDefault("date", "").trim();
        String time = data.getOrDefault("time", "").trim();

        if (doctorId.isEmpty() || date.isEmpty() || time.isEmpty()) {
            response.put("success", false);
            response.put("message", "Doctor ID, date, and time are required.");
            return response;
        }

        String entry = doctorId + "|" + date + "|" + time + "|AVAILABLE";
        if (slotExists(entry)) {
            response.put("success", false);
            response.put("message", "This slot already exists.");
            return response;
        }

        try (FileWriter fw = new FileWriter(SLOTS_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(entry);
            response.put("success", true);
            response.put("message", "Slot added successfully.");
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Failed to add slot.");
        }
        return response;
    }

    @GetMapping("/doctor/{docId}")
    public List<Map<String, String>> getSlots(@PathVariable("docId") String docId) {
        List<Map<String, String>> slots = new ArrayList<>();
        File file = new File(SLOTS_FILE);
        if (!file.exists()) return slots;

        try (BufferedReader br = new BufferedReader(new FileReader(SLOTS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 4 && parts[0].equals(docId)) {
                    Map<String, String> slot = new HashMap<>();
                    slot.put("doctorId", parts[0]);
                    slot.put("date", parts[1]);
                    slot.put("time", parts[2]);
                    slot.put("status", parts[3]);
                    slots.add(slot);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return slots;
    }

    @GetMapping("/all")
    public List<Map<String, String>> getAllSlots() {
        List<Map<String, String>> slots = new ArrayList<>();
        File file = new File(SLOTS_FILE);
        if (!file.exists()) return slots;

        try (BufferedReader br = new BufferedReader(new FileReader(SLOTS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 4) {
                    Map<String, String> slot = new HashMap<>();
                    slot.put("doctorId", parts[0]);
                    slot.put("date", parts[1]);
                    slot.put("time", parts[2]);
                    slot.put("status", parts[3]);
                    slots.add(slot);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return slots;
    }

    @DeleteMapping("/delete")
    public Map<String, Object> deleteSlot(@RequestParam(value = "docId", required = false) String docId,
                                          @RequestParam(value = "date", required = false) String date,
                                          @RequestParam(value = "time", required = false) String time,
                                          @RequestBody(required = false) Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();

        if (body != null) {
            if (docId == null || docId.isBlank()) {
                docId = body.getOrDefault("doctorId", body.getOrDefault("docId", ""));
            }
            if (date == null || date.isBlank()) {
                date = body.getOrDefault("date", "");
            }
            if (time == null || time.isBlank()) {
                time = body.getOrDefault("time", "");
            }
        }

        if (docId == null || docId.isBlank() || date == null || date.isBlank() || time == null || time.isBlank()) {
            response.put("success", false);
            response.put("message", "Doctor ID, date, and time are required.");
            return response;
        }

        List<String> lines = new ArrayList<>();
        boolean removed = false;

        try (BufferedReader br = new BufferedReader(new FileReader(SLOTS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String currentEntry = docId + "|" + date + "|" + time + "|AVAILABLE";
                if (!line.trim().equals(currentEntry)) {
                    lines.add(line);
                } else {
                    removed = true;
                }
            }
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error reading slot file.");
            return response;
        }

        if (removed) {
            try (PrintWriter out = new PrintWriter(new FileWriter(SLOTS_FILE))) {
                for (String line : lines) {
                    out.println(line);
                }
                response.put("success", true);
                response.put("message", "Slot deleted successfully.");
                return response;
            } catch (IOException e) {
                response.put("success", false);
                response.put("message", "Error writing slot file.");
                return response;
            }
        }

        response.put("success", false);
        response.put("message", "Slot not found.");
        return response;
    }

    @PutMapping("/update")
    public Map<String, Object> updateSlot(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String doctorId = body.getOrDefault("doctorId", body.getOrDefault("docId", "")).trim();
        String date = body.getOrDefault("date", "").trim();
        String time = body.getOrDefault("time", "").trim();
        String newDate = body.getOrDefault("newDate", "").trim();
        String newTime = body.getOrDefault("newTime", "").trim();

        if (doctorId.isEmpty() || date.isEmpty() || time.isEmpty() || newDate.isEmpty() || newTime.isEmpty()) {
            response.put("success", false);
            response.put("message", "Doctor ID, current slot, and new slot time are required.");
            return response;
        }

        String oldEntry = doctorId + "|" + date + "|" + time + "|AVAILABLE";
        String newEntry = doctorId + "|" + newDate + "|" + newTime + "|AVAILABLE";
        if (!oldEntry.equals(newEntry) && slotExists(newEntry)) {
            response.put("success", false);
            response.put("message", "The updated slot already exists.");
            return response;
        }

        List<String> lines = new ArrayList<>();
        boolean updated = false;
        try (BufferedReader br = new BufferedReader(new FileReader(SLOTS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!updated && line.trim().equals(oldEntry)) {
                    lines.add(newEntry);
                    updated = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error reading slot file.");
            return response;
        }

        if (!updated) {
            response.put("success", false);
            response.put("message", "Slot not found.");
            return response;
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(SLOTS_FILE))) {
            for (String line : lines) {
                out.println(line);
            }
            response.put("success", true);
            response.put("message", "Slot updated successfully.");
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error writing slot file.");
        }
        return response;
    }

    private boolean slotExists(String entry) {
        File file = new File(SLOTS_FILE);
        if (!file.exists()) return false;

        try (BufferedReader br = new BufferedReader(new FileReader(SLOTS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().equals(entry)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }

        return false;
    }
}
