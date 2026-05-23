package com.hospital.controllers;

import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/scheduling")
@CrossOrigin
public class SchedulingController {

    private static final String FILE_PATH = "data/appointments.txt";
    private static final String LOG_FILE_PATH = "data/appointment_logs.txt";
    private static final String PAT_FILE_PATH = "data/patients.txt";
    private static final String SLOTS_FILE_PATH = "data/slots.txt";

    @PostMapping("/book")
    public Map<String, Object> book(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String patientId = payload.getOrDefault("patientId", "").trim();
            String doctorId = payload.getOrDefault("doctorId", "").trim();
            String doctorName = payload.getOrDefault("doctorName", "").trim();
            String type = payload.getOrDefault("type", "NORMAL").trim();
            String timeSlot = payload.getOrDefault("timeSlot", "").trim();

            if (patientId.isEmpty() || doctorId.isEmpty() || doctorName.isEmpty()) {
                response.put("success", false);
                response.put("message", "Patient and doctor details are required.");
                return response;
            }

            String slotDate = "";
            String slotTime = "";
            if (!timeSlot.isEmpty()) {
                String[] slotParts = timeSlot.split("\\s*\\|\\s*", 2);
                slotDate = slotParts.length > 0 ? slotParts[0].trim() : "";
                slotTime = slotParts.length > 1 ? slotParts[1].trim() : "";
                if (!reserveSlot(doctorId, slotDate, slotTime)) {
                    response.put("success", false);
                    response.put("message", "Selected slot is no longer available.");
                    return response;
                }
            }

            String apptId = "APT" + (int) (Math.random() * 100000);
            long timestamp = System.currentTimeMillis();
            String record = apptId + "|" + patientId + "|" + doctorId + "|" + doctorName + "|" +
                    type + "|CONFIRMED|" + timestamp + "|" + slotDate + "|" + slotTime + System.lineSeparator();
            Files.write(Paths.get(FILE_PATH), record.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            response.put("success", true);
            response.put("appointmentId", apptId);
            response.put("message", "Appointment booked successfully.");
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Failed to book appointment.");
        }
        return response;
    }

    @PutMapping("/done/{apptId}")
    public String markApptDone(@PathVariable("apptId") String apptId) {
        try {
            File apptFile = new File(FILE_PATH);
            if (!apptFile.exists()) return "ERROR";

            List<String> updatedApptLines = new ArrayList<>();
            String patientId = "";
            String docName = "";
            String type = "";
            boolean found = false;

            for (String line : Files.readAllLines(Paths.get(FILE_PATH))) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split("\\|", -1);
                if (data.length >= 7 && data[0].equals(apptId)) {
                    patientId = data[1];
                    docName = data[3];
                    type = data[4];
                    String updatedLine = data[0] + "|" + data[1] + "|" + data[2] + "|" + data[3] + "|" +
                            data[4] + "|DONE|" + data[6];
                    if (data.length > 7) {
                        updatedLine += "|" + data[7] + "|" + (data.length > 8 ? data[8] : "");
                    }
                    updatedApptLines.add(updatedLine);
                    found = true;
                } else {
                    updatedApptLines.add(line);
                }
            }

            if (!found) return "ERROR";
            Files.write(Paths.get(FILE_PATH), updatedApptLines, StandardOpenOption.TRUNCATE_EXISTING);

            String patientName = "Unknown";
            File patFile = new File(PAT_FILE_PATH);
            if (patFile.exists()) {
                List<String> updatedPatLines = new ArrayList<>();
                for (String line : Files.readAllLines(Paths.get(PAT_FILE_PATH))) {
                    if (line.trim().isEmpty()) continue;
                    String[] pData = line.split("\\|", -1);
                    if (pData[0].equals(patientId)) {
                        patientName = pData[1];
                        String oldHistory = pData.length > 5 ? pData[5] : "";
                        String newRecord = "Completed " + type + " appointment with Dr. " + docName;
                        String newHistory = oldHistory.isEmpty() || oldHistory.equals("None")
                                ? newRecord
                                : oldHistory + ";;" + newRecord;
                        updatedPatLines.add(pData[0] + "|" + pData[1] + "|" + pData[2] + "|" + pData[3] + "|" +
                                (pData.length > 4 ? pData[4] : "N/A") + "|" + newHistory);
                    } else {
                        updatedPatLines.add(line);
                    }
                }
                Files.write(Paths.get(PAT_FILE_PATH), updatedPatLines, StandardOpenOption.TRUNCATE_EXISTING);
            }

            String dateTime = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date());
            String logEntry = dateTime + "|" + apptId + "|" + patientId + "|" + patientName + "|" + docName + System.lineSeparator();
            Files.write(Paths.get(LOG_FILE_PATH), logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return "SUCCESS";
        } catch (IOException e) {
            return "ERROR";
        }
    }

    @GetMapping("/logs")
    public List<Map<String, String>> getLogs() {
        List<Map<String, String>> logs = new ArrayList<>();
        try {
            File file = new File(LOG_FILE_PATH);
            if (!file.exists()) return logs;

            for (String line : Files.readAllLines(Paths.get(LOG_FILE_PATH))) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split("\\|", -1);
                if (data.length >= 5) {
                    Map<String, String> map = new HashMap<>();
                    map.put("dateTime", data[0]);
                    map.put("apptId", data[1]);
                    map.put("patientId", data[2]);
                    map.put("patientName", data[3]);
                    map.put("doctorName", data[4]);
                    logs.add(map);
                }
            }
        } catch (IOException ignored) {
        }
        Collections.reverse(logs);
        return logs;
    }

    @GetMapping("/patient/{patientId}")
    public List<Map<String, Object>> getAppointmentsByPatient(@PathVariable("patientId") String patientId) {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) return list;

            for (String line : Files.readAllLines(Paths.get(FILE_PATH))) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split("\\|", -1);
                if (data.length >= 7 && data[1].trim().equals(patientId)) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("apptId", data[0].trim());
                    map.put("doctorName", data[3].trim());
                    map.put("type", data[4].trim());
                    map.put("status", data[5].trim());
                    map.put("slotDate", data.length > 7 ? data[7].trim() : "");
                    map.put("slotTime", data.length > 8 ? data[8].trim() : "");
                    long bookedTime = parseTimestamp(data[6]);
                    map.put("canDelete", (System.currentTimeMillis() - bookedTime) <= 14400000);
                    list.add(map);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    @DeleteMapping("/cancel/{apptId}")
    public Map<String, Object> cancelAppointment(@PathVariable("apptId") String apptId) {
        Map<String, Object> response = new HashMap<>();
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                response.put("success", false);
                response.put("message", "No appointment data found.");
                return response;
            }

            List<String> updatedLines = new ArrayList<>();
            String[] cancelledRecord = null;

            for (String line : Files.readAllLines(Paths.get(FILE_PATH))) {
                if (line.trim().isEmpty()) continue;
                if (!line.startsWith(apptId + "|")) {
                    updatedLines.add(line);
                } else {
                    cancelledRecord = line.split("\\|", -1);
                }
            }

            Files.write(Paths.get(FILE_PATH), updatedLines, StandardOpenOption.TRUNCATE_EXISTING);
            if (cancelledRecord != null && cancelledRecord.length > 8 &&
                    !cancelledRecord[7].isBlank() && !cancelledRecord[8].isBlank()) {
                restoreSlot(cancelledRecord[2], cancelledRecord[7], cancelledRecord[8]);
            }
            if (cancelledRecord != null) {
                response.put("success", true);
                response.put("message", "Appointment cancelled. You can reschedule it now.");
                response.put("canReschedule", true);
                response.put("apptId", cancelledRecord[0]);
                response.put("patientId", cancelledRecord[1]);
                response.put("doctorId", cancelledRecord.length > 2 ? cancelledRecord[2] : "");
                response.put("doctorName", cancelledRecord.length > 3 ? cancelledRecord[3] : "");
                response.put("type", cancelledRecord.length > 4 ? cancelledRecord[4] : "NORMAL");
                return response;
            }
            response.put("success", false);
            response.put("message", "Appointment not found.");
            return response;
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Failed to cancel appointment.");
            return response;
        }
    }

    @GetMapping("/all")
    public List<Map<String, Object>> getAllAppointments() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) return list;

            for (String line : Files.readAllLines(Paths.get(FILE_PATH))) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split("\\|", -1);
                if (data.length >= 7) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("apptId", data[0].trim());
                    map.put("patientId", data[1].trim());
                    map.put("doctorId", data[2].trim());
                    map.put("doctorName", data[3].trim());
                    map.put("type", data[4].trim());
                    map.put("status", data[5].trim());
                    map.put("timestamp", parseTimestamp(data[6]));
                    map.put("slotDate", data.length > 7 ? data[7].trim() : "");
                    map.put("slotTime", data.length > 8 ? data[8].trim() : "");
                    list.add(map);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private long parseTimestamp(String value) {
        try {
            return Long.parseLong(value == null ? "0" : value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private boolean reserveSlot(String doctorId, String date, String time) throws IOException {
        if (date.isBlank() || time.isBlank()) return true;
        File file = new File(SLOTS_FILE_PATH);
        if (!file.exists()) return false;

        List<String> updatedLines = new ArrayList<>();
        boolean removed = false;
        for (String line : Files.readAllLines(Paths.get(SLOTS_FILE_PATH))) {
            String expected = doctorId + "|" + date + "|" + time + "|AVAILABLE";
            if (!removed && line.trim().equals(expected)) {
                removed = true;
                continue;
            }
            updatedLines.add(line);
        }

        if (removed) {
            Files.write(Paths.get(SLOTS_FILE_PATH), updatedLines, StandardOpenOption.TRUNCATE_EXISTING);
        }
        return removed;
    }

    private void restoreSlot(String doctorId, String date, String time) throws IOException {
        if (doctorId == null || doctorId.isBlank() || date == null || date.isBlank() || time == null || time.isBlank()) {
            return;
        }

        String slotEntry = doctorId + "|" + date + "|" + time + "|AVAILABLE";
        File file = new File(SLOTS_FILE_PATH);
        if (file.exists()) {
            for (String line : Files.readAllLines(Paths.get(SLOTS_FILE_PATH))) {
                if (line.trim().equals(slotEntry)) {
                    return;
                }
            }
        }

        Files.write(Paths.get(SLOTS_FILE_PATH), (slotEntry + System.lineSeparator()).getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
