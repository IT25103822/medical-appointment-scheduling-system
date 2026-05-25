package com.hospital.models;

public class Appointment {
    private String patientId;
    private String doctorId;
    private String doctorName;
    private String type; // Normal or Emergency
    private String status; // Pending or Confirmed

    public Appointment() {}

    public Appointment(String patientId, String doctorId, String doctorName, String type, String status) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.type = type;
        this.status = status;
    }

    // Getters and Setters
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
