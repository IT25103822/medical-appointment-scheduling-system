package com.hospital.models;

public class MedicalRecord {
    private String recordId;
    private String patientId;
    private String providerId;
    private String appointmentId;
    private String notes;
    private String labResults;
    private String createdAt;
    private boolean archived;

    public MedicalRecord() {}

    public MedicalRecord(String recordId, String patientId, String providerId,
                         String appointmentId, String notes, String labResults,
                         String createdAt, boolean archived) {
        this.recordId = recordId;
        this.patientId = patientId;
        this.providerId = providerId;
        this.appointmentId = appointmentId;
        this.notes = notes;
        this.labResults = labResults;
        this.createdAt = createdAt;
        this.archived = archived;
    }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getLabResults() { return labResults; }
    public void setLabResults(String labResults) { this.labResults = labResults; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
}
