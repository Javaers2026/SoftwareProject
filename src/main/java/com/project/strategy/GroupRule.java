package com.project.strategy;

import com.project.*;

public class GroupRule implements BookingRuleStrategy {
    
    @Override
    public boolean isValid(Appointment appointment){
        if(appointment.getType() == AppointmentType.GROUP){
            return appointment.getParticipants() <= 10;
        }
        return true;
    }
}
