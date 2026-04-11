package com.project.strategy;

import com.project.Appointment;
import com.project.AppointmentType;
import java.time.Duration;

public class FollowUpRule implements BookingRuleStrategy{
    private static final long MAX_MINUTES = 60;

    @Override
    public boolean isValid(Appointment appointment) {
        if (appointment.getType() != AppointmentType.FOLLOW_UP) {
            return true;
        }
        long minutes = Duration.between(appointment.getStart(), appointment.getEnd()).toMinutes();
        return minutes <= MAX_MINUTES && appointment.getParticipants() == 1;
    }
}
