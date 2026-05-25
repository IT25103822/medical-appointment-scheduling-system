package com.hospital.services;

import org.springframework.stereotype.Component;

@Component
public class NormalScheduling implements SchedulingStrategy {
    public String book() { return "Normal appointment scheduled."; }
}