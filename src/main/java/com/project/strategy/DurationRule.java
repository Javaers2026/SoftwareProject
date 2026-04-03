package com.project.strategy;

import com.project.Appointment;
import java.time.Duration;

public class DurationRule implements BookingRuleStrategy {
    @Override
    public boolean isValid(Appointment appointment) {
        long minutes = Duration.between(appointment.getStart(), appointment.getEnd()).toMinutes();
        return minutes > 0 && minutes <= 120;
    }
}