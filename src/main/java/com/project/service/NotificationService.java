package com.project.service;

import com.project.Appointment;
import com.project.User;
import com.project.observer.NotifacationManager;

public class NotificationService {
    private NotifacationManager notifacationManager;

    public NotificationService(NotifacationManager notifacationManager) {
        this.notifacationManager = notifacationManager;
    }

    public void sendReminder(Appointment appointment, User user) {
        String message = "Reminder: You have an appointment at " + appointment.getFormattedStart();
        notifacationManager.notifyAll(user, message);
    }
}

