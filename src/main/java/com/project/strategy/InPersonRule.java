package com.project.strategy;

import com.project.Appointment;
import com.project.AppointmentType;

public class InPersonRule implements BookingRuleStrategy{
    private static final int MAX_PARTICIPANTS = 8;

    @Override
    public boolean isValid(Appointment appointment) {
        if (appointment.getType() != AppointmentType.IN_PERSON) {
            return true;
        }
        return appointment.getParticipants() >= 1
                && appointment.getParticipants() <= MAX_PARTICIPANTS;
    }
}
