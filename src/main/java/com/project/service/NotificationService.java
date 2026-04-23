package com.project.service;

import com.project.Appointment;
import com.project.User;
import com.project.observer.NotificationManager;

public class NotificationService {
    private NotificationManager notificationManager;

    public NotificationService(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    public void sendReminder(Appointment appointment, User user) {
        String message = "Reminder: You have an appointment at " + appointment.getFormattedStart();
        notificationManager.notifyAll(user, message);
    }
}
