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
@RequestMapping("/api/billing")
@CrossOrigin
public class BillingController {

    private static final String BILL_FILE = "data/bills.txt";
    private static final String APPT_FILE = "data/appointments.txt";
    private static final String PAT_FILE = "data/patients.txt";

    @GetMapping("/pending")
    public List<Map<String, String>> getPendingBills() {
        List<Map<String, String>> pendingList = new ArrayList<>();
        try {
            Set<String> billedAppts = new HashSet<>();
            File billFile = new File(BILL_FILE);
            if (billFile.exists()) {
                for (String line : Files.readAllLines(Paths.get(BILL_FILE))) {
                    if (!line.trim().isEmpty()) {
                        String[] data = line.split("\\|", -1);
                        if (data.length > 1) {
                            billedAppts.add(data[1]);
                        }
                    }
                }
            }

            File appointmentFile = new File(APPT_FILE);
            if (appointmentFile.exists()) {
                for (String line : Files.readAllLines(Paths.get(APPT_FILE))) {
                    if (line.trim().isEmpty()) continue;
                    String[] data = line.split("\\|", -1);
                    if (data.length >= 7 && data[5].equals("DONE") && !billedAppts.contains(data[0])) {
                        Map<String, String> map = new HashMap<>();
                        map.put("apptId", data[0]);
                        map.put("patientId", data[1]);
                        map.put("doctorName", data[3]);
                        pendingList.add(map);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return pendingList;
    }

    @PostMapping("/pay")
    public Map<String, Object> processPayment(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String apptId = payload.getOrDefault("apptId", "").trim();
            String patientId = payload.getOrDefault("patientId", "").trim();
            if (apptId.isEmpty() || patientId.isEmpty()) {
                response.put("success", false);
                response.put("message", "Appointment ID and patient ID are required.");
                return response;
            }

            String billId = "INV" + (int) (Math.random() * 10000);
            String date = new SimpleDateFormat("yyyy-MM-dd hh:mm a").format(new Date());
            String totalAmount = payload.getOrDefault("total", "0");
            String note = sanitize(payload.getOrDefault("note", ""));

            String record = billId + "|" + apptId + "|" + patientId + "|" + payload.getOrDefault("doctorName", "") + "|" +
                    payload.getOrDefault("docFee", "0") + "|" + payload.getOrDefault("hospFee", "0") + "|" +
                    payload.getOrDefault("extraFee", "0") + "|" + totalAmount + "|" + date + "|PAID|" + note + System.lineSeparator();
            Files.write(Paths.get(BILL_FILE), record.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            File patientFile = new File(PAT_FILE);
            if (patientFile.exists()) {
                List<String> updatedPatLines = new ArrayList<>();
                for (String line : Files.readAllLines(Paths.get(PAT_FILE))) {
                    if (line.trim().isEmpty()) continue;
                    String[] data = line.split("\\|", -1);
                    if (data[0].equals(patientId)) {
                        String oldHistory = data.length > 5 ? data[5] : "";
                        String newRecord = "ðŸ’³ Paid Invoice " + billId + " (Amount: LKR " + totalAmount + ")";
                        String newHistory = oldHistory.isEmpty() || oldHistory.equals("None")
                                ? newRecord
                                : oldHistory + ";;" + newRecord;
                        updatedPatLines.add(data[0] + "|" + data[1] + "|" + data[2] + "|" + data[3] + "|" +
                                (data.length > 4 ? data[4] : "N/A") + "|" + newHistory);
                    } else {
                        updatedPatLines.add(line);
                    }
                }
                Files.write(Paths.get(PAT_FILE), updatedPatLines, StandardOpenOption.TRUNCATE_EXISTING);
            }

            response.put("success", true);
            response.put("billId", billId);
            response.put("message", "Invoice generated successfully.");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to generate invoice.");
        }
        return response;
    }

    @GetMapping("/all")
    public List<Map<String, String>> getAllBills() {
        return readBillsFromFile(null);
    }

    @GetMapping("/patient/{patientId}")
    public List<Map<String, String>> getBillsByPatient(@PathVariable("patientId") String patientId) {
        return readBillsFromFile(patientId);
    }

    @PutMapping("/update/{billId}")
    public Map<String, Object> updateBill(@PathVariable("billId") String billId, @RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            File file = new File(BILL_FILE);
            if (!file.exists()) {
                res.put("success", false);
                res.put("message", "No bills found.");
                return res;
            }

            List<String> lines = Files.readAllLines(Paths.get(BILL_FILE));
            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().isEmpty()) continue;
                String[] data = lines.get(i).split("\\|", -1);
                if (data.length >= 8 && data[0].equals(billId)) {
                    if (body.containsKey("docFee")) data[4] = body.get("docFee");
                    if (body.containsKey("hospFee")) data[5] = body.get("hospFee");
                    if (body.containsKey("extraFee")) data[6] = body.get("extraFee");
                    try {
                        double total = Double.parseDouble(data[4]) + Double.parseDouble(data[5]) + Double.parseDouble(data[6]);
                        data[7] = String.valueOf((int) total);
                    } catch (NumberFormatException ignored) {
                    }
                    lines.set(i, String.join("|", data));
                    found = true;
                    break;
                }
            }

            if (!found) {
                res.put("success", false);
                res.put("message", "Invoice not found: " + billId);
                return res;
            }

            Files.write(Paths.get(BILL_FILE), lines);
            res.put("success", true);
            res.put("message", "Invoice " + billId + " updated successfully.");
        } catch (IOException e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    @DeleteMapping("/void/{billId}")
    public Map<String, Object> voidBill(@PathVariable("billId") String billId) {
        Map<String, Object> res = new HashMap<>();
        try {
            File file = new File(BILL_FILE);
            if (!file.exists()) {
                res.put("success", false);
                res.put("message", "No bills found.");
                return res;
            }

            List<String> lines = Files.readAllLines(Paths.get(BILL_FILE));
            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().isEmpty()) continue;
                String[] data = lines.get(i).split("\\|", -1);
                if (data.length >= 8 && data[0].equals(billId)) {
                    String[] extended = Arrays.copyOf(data, Math.max(data.length, 10));
                    extended[9] = "VOID";
                    lines.set(i, String.join("|", extended));
                    found = true;
                    break;
                }
            }

            if (!found) {
                res.put("success", false);
                res.put("message", "Invoice not found: " + billId);
                return res;
            }

            Files.write(Paths.get(BILL_FILE), lines);
            res.put("success", true);
            res.put("message", "Invoice " + billId + " voided successfully.");
        } catch (IOException e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    private List<Map<String, String>> readBillsFromFile(String filterPatientId) {
        List<Map<String, String>> list = new ArrayList<>();
        try {
            File file = new File(BILL_FILE);
            if (!file.exists()) return list;

            for (String line : Files.readAllLines(Paths.get(BILL_FILE))) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split("\\|", -1);
                if (data.length >= 9 && (filterPatientId == null || data[2].equals(filterPatientId))) {
                    Map<String, String> map = new HashMap<>();
                    map.put("billId", data[0]);
                    map.put("apptId", data[1]);
                    map.put("patientId", data[2]);
                    map.put("doctorName", data[3]);
                    map.put("docFee", data[4]);
                    map.put("hospFee", data[5]);
                    map.put("extraFee", data[6]);
                    map.put("total", data[7]);
                    map.put("date", data[8]);
                    map.put("status", data.length > 9 && !data[9].isEmpty() ? data[9] : "PAID");
                    map.put("note", data.length > 10 ? data[10] : "");
                    list.add(map);
                }
            }
        } catch (Exception ignored) {
        }
        Collections.reverse(list);
        return list;
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace("|", "-").replaceAll("[\\r\\n]+", " ").trim();
    }
}
