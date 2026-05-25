package com.hospital.models;

public class Patient extends User {
    private String age;
    private String address;
    private String mobile;
    private String medicalHistory;

    // 1. Empty Constructor (Spring Boot වලට අත්‍යවශ්‍යයි)
    public Patient() {
        super();
    }

    // 2. Parameterized Constructor
    public Patient(String id, String name, String age, String address, String mobile, String medicalHistory) {
        super(id, name);
        this.age = age;
        this.address = address;
        this.mobile = mobile;
        this.medicalHistory = medicalHistory;
    }

    public Patient(String p001, String kamalPerera, String noAllergies) {
    }

    // Getters and Setters
    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }
}
