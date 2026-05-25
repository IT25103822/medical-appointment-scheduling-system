package com.hospital.services;

import com.hospital.models.Doctor;
import java.util.List;

public interface IScheduleManager {
    String addDoctor(Doctor doctor);
    List<Doctor> getAllDoctors();
    String updateDoctor(String id, Doctor doctor); // අලුතින් දැම්මා
    String deleteDoctor(String id);                // අලුතින් දැම්මා
}