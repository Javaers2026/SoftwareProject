package com.project.strategy;

import com.project.*;
import java.time.Duration;

public class UrgentRule implements BookingRuleStrategy {
    
    @Override
    public boolean isValid(Appointment appointment){
        if(appointment.getType() == AppointmentType.URGENT){
            long minutes = Duration.between(appointment.getStart(), appointment.getEnd()).toMinutes();
            return  minutes <=30;
        }
        return true;
    }
}
