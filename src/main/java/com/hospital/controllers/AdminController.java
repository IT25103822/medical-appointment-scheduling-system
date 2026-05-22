package com.hospital.controllers;

import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private static final String USERS_FILE  = "data/admin_users.txt";
    private static final String LOGS_FILE   = "data/system_logs.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ──────────────────────────────────────────────
    //  SYSTEM USER CRUD
    // ──────────────────────────────────────────────

    // CREATE – Add new user or role
    @PostMapping("/users/add")
    public Map<String, Object> addUser(@RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            String userId   = "USR" + (1000 + new Random().nextInt(9000));
            String username = body.getOrDefault("username", "").replace("|", "-");
            String email    = body.getOrDefault("email", "").replace("|", "-");
            String role     = body.getOrDefault("role", "STAFF").replace("|", "-");
            String password = body.getOrDefault("password", "changeme").replace("|", "-");
            String createdAt = LocalDateTime.now().format(FORMATTER);

            if (username.isEmpty() || email.isEmpty()) {
                res.put("success", false);
                res.put("message", "Username and email are required.");
                return res;
            }

            // Check duplicate username
            File file = new File(USERS_FILE);
            if (file.exists()) {
                for (String line : Files.readAllLines(Path.of(USERS_FILE))) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split("\\|");
                    if (parts.length >= 2 && parts[1].equalsIgnoreCase(username)) {
                        res.put("success", false);
                        res.put("message", "Username already exists.");
                        return res;
                    }
                }
            }

            String line = String.join("|", userId, username, email, role, password, "true", createdAt);
            Files.writeString(Path.of(USERS_FILE), line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            logAction("SYSTEM", "USER_CREATED", "Created user: " + username + " (" + role + ")");

            res.put("success", true);
            res.put("userId", userId);
            res.put("message", "User added successfully.");
        } catch (IOException e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    // READ – Get all system users
    @GetMapping("/users/all")
    public Map<String, Object> getAllUsers() {
        Map<String, Object> res = new HashMap<>();
        List<Map<String, String>> users = new ArrayList<>();
        try {
            File file = new File(USERS_FILE);
            if (file.exists()) {
                for (String line : Files.readAllLines(Path.of(USERS_FILE))) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split("\\|", -1);
                    if (parts.length >= 7) {
                        Map<String, String> user = new LinkedHashMap<>();
                        user.put("userId",    parts[0]);
                        user.put("username",  parts[1]);
                        user.put("email",     parts[2]);
                        user.put("role",      parts[3]);
                        user.put("active",    parts[5]);
                        user.put("createdAt", parts[6]);
                        users.add(user);
                    }
                }
            }
            res.put("success", true);
            res.put("users", users);
        } catch (IOException e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    // READ – View system logs and reports
    @GetMapping("/logs")
    public Map<String, Object> getLogs(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        Map<String, Object> res = new HashMap<>();
        List<Map<String, String>> logs = new ArrayList<>();
        try {
            File file = new File(LOGS_FILE);
            if (file.exists()) {
                List<String> lines = Files.readAllLines(Path.of(LOGS_FILE));
                int start = Math.max(0, lines.size() - limit);
                for (int i = lines.size() - 1; i >= start; i--) {
                    String line = lines.get(i);
                    if (line.isBlank()) continue;
                    String[] parts = line.split("\\|", -1);
                    if (parts.length >= 4) {
                        Map<String, String> log = new LinkedHashMap<>();
                        log.put("timestamp", parts[0]);
                        log.put("userId",    parts[1]);
                        log.put("action",    parts[2]);
                        log.put("details",   parts[3]);
                        logs.add(log);
                    }
                }
            }
            res.put("success", true);
            res.put("logs", logs);
        } catch (IOException e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    // UPDATE – Modify permissions and role
    @PutMapping("/users/update/{userId}")
    public Map<String, Object> updateUser(@PathVariable("userId") String userId,
                                           @RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            File file = new File(USERS_FILE);
            if (!file.exists()) {
                res.put("success", false);
                res.put("message", "No users found.");
                return res;
            }
            List<String> lines = Files.readAllLines(Path.of(USERS_FILE));
            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).isBlank()) continue;
                String[] parts = lines.get(i).split("\\|", -1);
                if (parts.length >= 7 && parts[0].equals(userId)) {
                    if (body.containsKey("role"))     parts[3] = body.get("role").replace("|", "-");
                    if (body.containsKey("email"))    parts[2] = body.get("email").replace("|", "-");
                    if (body.containsKey("password")) parts[4] = body.get("password").replace("|", "-");
                    lines.set(i, String.join("|", parts));
                    found = true;
                    logAction("ADMIN", "USER_UPDATED", "Updated user: " + parts[1]);
                    break;
                }
            }
            if (!found) {
                res.put("success", false);
                res.put("message", "User not found: " + userId);
                return res;
            }
            Files.write(Path.of(USERS_FILE), lines);
            res.put("success", true);
            res.put("message", "User updated successfully.");
        } catch (IOException e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    // DELETE – Deactivate user (soft delete)
    @DeleteMapping("/users/deactivate/{userId}")
    public Map<String, Object> deactivateUser(@PathVariable("userId") String userId) {
        Map<String, Object> res = new HashMap<>();
        try {
            File file = new File(USERS_FILE);
            if (!file.exists()) {
                res.put("success", false);
                res.put("message", "No users found.");
                return res;
            }
            List<String> lines = Files.readAllLines(Path.of(USERS_FILE));
            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).isBlank()) continue;
                String[] parts = lines.get(i).split("\\|", -1);
                if (parts.length >= 7 && parts[0].equals(userId)) {
                    parts[5] = "false";
                    lines.set(i, String.join("|", parts));
                    found = true;
                    logAction("ADMIN", "USER_DEACTIVATED", "Deactivated user: " + parts[1]);
                    break;
                }
            }
            if (!found) {
                res.put("success", false);
                res.put("message", "User not found: " + userId);
                return res;
            }
            Files.write(Path.of(USERS_FILE), lines);
            res.put("success", true);
            res.put("message", "User deactivated successfully.");
        } catch (IOException e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    // ──────────────────────────────────────────────
    //  INTERNAL LOGGING UTILITY
    // ──────────────────────────────────────────────
    public static void logAction(String userId, String action, String details) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String entry = String.join("|", timestamp, userId, action,
                    details.replace("|", "-")) + System.lineSeparator();
            Files.writeString(Path.of(LOGS_FILE), entry,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }
}
