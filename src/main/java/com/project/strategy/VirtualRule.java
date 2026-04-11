package com.project.strategy;

import com.project.Appointment;
import com.project.AppointmentType;

public class VirtualRule implements BookingRuleStrategy{
    private static final int MIN_PARTICIPANTS = 2;

    private static final int MAX_PARTICIPANTS = 10;

    @Override
    public boolean isValid(Appointment appointment) {
        if (appointment.getType() != AppointmentType.VIRTUAL) {
            return true;
        }
        int p = appointment.getParticipants();
        return p >= MIN_PARTICIPANTS && p <= MAX_PARTICIPANTS;
    }
}
