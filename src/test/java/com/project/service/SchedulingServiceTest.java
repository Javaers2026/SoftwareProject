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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
        verify(notificationService, times(1)).sendReminder(eq(appointment), argThat(User::isAdmin));
    }

    @Test
    void testDueReminderSchedulerSendsUserAndAdminNotification() {
        NotificationService notificationService = mock(NotificationService.class);
        SchedulingService schedulingService = new SchedulingService(notificationService);
        schedulingService.clearSlotsForTesting();

        assertTrue(schedulingService.login("admin", "admin123"));
        schedulingService.addTimeSlot(new TimeSlot(
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusMinutes(40)
        ));
        schedulingService.logout();

        assertTrue(schedulingService.login("user", "user123"));
        TimeSlot slot = schedulingService.getAvailableSlots().get(0);
        assertTrue(schedulingService.book(slot, 1, AppointmentType.INDIVIDUAL));

        Appointment appointment = schedulingService.getAppointments().get(0);
        schedulingService.checkAndSendRemindersForDueAppointments();

        verify(notificationService, times(1)).sendReminder(
                eq(appointment),
                argThat(u -> !u.isAdmin() && u.getUsername().equals("user"))
        );
        verify(notificationService, times(1)).sendReminder(
                eq(appointment),
                argThat(User::isAdmin)
        );
        assertTrue(appointment.isReminderSent());
    }

    @Test
    void testSaveAndLoadAppointments() throws Exception {
        SchedulingService service = new SchedulingService();
        service.clearSlotsForTesting();
        assertTrue(service.login("user", "user123"));
        TimeSlot slot = new TimeSlot(
                LocalDateTime.now().plusMinutes(10).withSecond(0).withNano(0),
                LocalDateTime.now().plusMinutes(40).withSecond(0).withNano(0)
        );
        assertTrue(service.addTimeSlot(slot));
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

    @Test
    void testSaveAndLoadSlots() throws Exception {
        SchedulingService service = new SchedulingService();
        service.clearSlotsForTesting();

        TimeSlot slot = new TimeSlot(
                LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0)
        );
        assertTrue(service.addTimeSlot(slot));

        Path tempFile = Files.createTempFile("slots", ".txt");
        try {
            service.saveSlots(tempFile.toString());

            SchedulingService loadedService = new SchedulingService();
            loadedService.clearSlotsForTesting();
            loadedService.loadSlots(tempFile.toString());

            assertEquals(1, loadedService.getAvailableSlots().size());
            assertEquals(slot.getStart(), loadedService.getAvailableSlots().get(0).getStart());
            assertEquals(slot.getEnd(), loadedService.getAvailableSlots().get(0).getEnd());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}