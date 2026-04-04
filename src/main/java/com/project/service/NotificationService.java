package com.project.service;

import com.project.Appointment;
import com.project.User;

public class NotificationService {
    public void sendReminder(Appointment appointment, User user){
        String message = "Reminder: You have an appointment at " + appointment.getStart();
        appointment.notifyObservers(user, message);
    }
}
