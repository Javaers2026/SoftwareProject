package com.project.service;

import com.project.Appointment;
import com.project.AppointmentType;
import com.project.User;
import com.project.observer.NotifacationManager;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NotificationServiceTest {

    @Test
    void testNotificationManagerReceivesReminder() {
        NotifacationManager manager = mock(NotifacationManager.class);
        NotificationService notificationService = new NotificationService(manager);
        User user = new User("ayman", "123", false);
        Appointment appointment = new Appointment(
                "1",
                "ayman",
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusHours(1),
                1,
                AppointmentType.INDIVIDUAL
        );

        notificationService.sendReminder(appointment, user);

        verify(manager, times(1)).notifyAll(user, "Reminder: You have an appointment at " + appointment.getFormattedStart());
    }
}
