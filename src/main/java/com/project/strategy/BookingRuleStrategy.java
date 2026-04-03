package com.project.strategy;

import com.project.Appointment;

public interface BookingRuleStrategy {
    boolean isValid(Appointment appointment);
}