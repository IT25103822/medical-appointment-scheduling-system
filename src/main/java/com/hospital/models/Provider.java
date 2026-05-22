package com.hospital.models;

public class Provider {
    private String id;
    private String name;
    private String specialization;

    // ✅ මේ හිස් Constructor එක අනිවාර්යයෙන්ම ඕනේ!
    public Provider() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
}

