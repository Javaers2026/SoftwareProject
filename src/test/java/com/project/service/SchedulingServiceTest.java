package com.project.service;

import com.project.Appointment;
import com.project.AppointmentType;
import com.project.TimeSlot;
import com.project.User;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        verify(notificationService, times(1)).sendReminder(appointment, user);
    }

    @Test
    void testSaveAndLoadAppointments() throws Exception {
        SchedulingService service = new SchedulingService();
        assertTrue(service.login("user", "user123"));
        TimeSlot slot = service.getAvailableSlots().get(0);
        assertTrue(service.book(slot, 1, AppointmentType.INDIVIDUAL));

        Path tempFile = Files.createTempFile("appointments", ".txt");
        try {
            service.saveAppointments(tempFile.toString());

            SchedulingService loadedService = new SchedulingService();
            loadedService.loadAppointments(tempFile.toString());
            assertTrue(loadedService.login("user", "user123"));
            assertEquals(1, loadedService.getAppointments().size());
            assertEquals("user", loadedService.getAppointments().get(0).getUserId());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
