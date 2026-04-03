package com.project.strategy;

import com.project.Appointment;

public class ParticipantLimitRule implements BookingRuleStrategy {
    private int max;

    public ParticipantLimitRule(int max) {
        this.max = max;
    }

    @Override
    public boolean isValid(Appointment appointment) {
        return appointment.getParticipants() <= max;
    }
}