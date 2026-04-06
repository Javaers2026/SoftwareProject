package com.project.service;

import com.project.Appointment;
import com.project.AppointmentType;
import com.project.User;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

public class SchedulingServiceTest {

    @Test
void testReminderSent() {

    NotificationService notificationService = mock(NotificationService.class);

    SchedulingService schedulingService = new SchedulingService(notificationService);

    User user = new User("ayman", "123", false);

    Appointment appointment = new Appointment(
            "1",
            "ayman",
            LocalDateTime.now().plusMinutes(10), 
            LocalDateTime.now().plusHours(1),
            1,
            AppointmentType.INDIVIDUAL   
    );

    schedulingService.checkAndSendReminders(appointment, user);

    verify(notificationService, times(1))
            .sendReminder(appointment, user);
}
}