package com.project.strategy;

import com.project.Appointment;
import com.project.AppointmentType;
import java.time.Duration;

public class AssessmentRule implements BookingRuleStrategy {
    private static final long MAX_MINUTES = 90;

    private static final int MAX_PARTICIPANTS = 3;

    @Override
    public boolean isValid(Appointment appointment) {
        if (appointment.getType() != AppointmentType.ASSESSMENT) {
            return true;
        }
        long minutes = Duration.between(appointment.getStart(), appointment.getEnd()).toMinutes();
        return minutes <= MAX_MINUTES && appointment.getParticipants() <= MAX_PARTICIPANTS;
    }
}
