package com.project.strategy;

import com.project.*;

public class IndividualRule implements BookingRuleStrategy {
    
    @Override
    public boolean isValid(Appointment appointment){
        if(appointment.getType() == AppointmentType.INDIVIDUAL){
            return appointment.getParticipants() == 1;
        }
        return true;
    }
}
