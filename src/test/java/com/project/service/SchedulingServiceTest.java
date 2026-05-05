package com.project.service;

import com.project.Appointment;
import com.project.AppointmentType;
import com.project.TimeSlot;
import com.project.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SchedulingServiceTest {

    private SchedulingService service;
    private TimeSlot futureSlot;

    @BeforeEach
    void setUp() {
        service = new SchedulingService();
        service.clearSlotsForTesting();
        futureSlot = new TimeSlot(
                LocalDateTime.now().plusDays(1).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(1).plusHours(1).withSecond(0).withNano(0)
        );
    }

    @Test
    void login_validAdminCredentials_returnsTrue() {
        assertTrue(service.login("admin", "admin123"));
        assertNotNull(service.getCurrentUser());
        assertTrue(service.getCurrentUser().isAdmin());
    }

    @Test
    void login_validUserCredentials_returnsTrue() {
        assertTrue(service.login("user", "user123"));
        assertNotNull(service.getCurrentUser());
        assertFalse(service.getCurrentUser().isAdmin());
    }

    @Test
    void login_wrongPassword_returnsFalse() {
        assertFalse(service.login("admin", "wrongpass"));
        assertNull(service.getCurrentUser());
    }

    @Test
    void login_unknownUsername_returnsFalse() {
        assertFalse(service.login("nobody", "admin123"));
        assertNull(service.getCurrentUser());
    }

    @Test
    void logout_clearsCurrentUser() {
        service.login("user", "user123");
        service.logout();
        assertNull(service.getCurrentUser());
    }

    @Test
    void logout_withoutLogin_doesNotThrow() {
        assertDoesNotThrow(() -> service.logout());
    }

    @Test
    void registerUser_newUsername_returnsTrue() {
        // Use a unique name so a leftover users.txt from a prior run never causes a clash
        String unique = "testuser_" + System.nanoTime();
        assertTrue(service.registerUser(unique, "pass123", "test@example.com"));
        assertTrue(service.login(unique, "pass123"));
    }

    @Test
    void registerUser_duplicateUsername_returnsFalse() {
        assertFalse(service.registerUser("admin", "anything", "admin@example.com"));
    }

    @Test
    void registerUser_emptyUsername_returnsFalse() {
        assertFalse(service.registerUser("", "pass", "bad@example.com"));
    }

    @Test
    void registerUser_emptyPassword_returnsFalse() {
        assertFalse(service.registerUser("someone", "", "bad@example.com"));
    }

    @Test
    void addTimeSlot_valid_appearsInAvailableSlots() {
        service.addTimeSlot(futureSlot);
        List<TimeSlot> available = service.getAvailableSlots();
        assertEquals(1, available.size());
        assertTrue(available.get(0).isAvailable());
    }

    @Test
    void addTimeSlot_nullSlot_returnsFalse() {
        assertFalse(service.addTimeSlot(null));
    }

    @Test
    void addTimeSlot_endBeforeStart_returnsFalse() {
        TimeSlot bad = new TimeSlot(
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1)
        );
        assertFalse(service.addTimeSlot(bad));
    }

    @Test
    void addTimeSlot_duplicate_returnsFalse() {
        service.addTimeSlot(futureSlot);
        assertFalse(service.addTimeSlot(futureSlot));
    }

    @Test
    void getAvailableSlots_afterBooking_slotNotAvailable() {
        service.addTimeSlot(futureSlot);
        service.login("user", "user123");
        service.book(futureSlot, 1, AppointmentType.INDIVIDUAL);
        assertTrue(service.getAvailableSlots().isEmpty());
    }

    @Test
    void book_withoutLogin_returnsFalse() {
        service.addTimeSlot(futureSlot);
        assertFalse(service.book(futureSlot, 1, AppointmentType.INDIVIDUAL));
    }

    @Test
    void book_validSlotAndRules_returnsTrue() {
        service.addTimeSlot(futureSlot);
        service.login("user", "user123");
        assertTrue(service.book(futureSlot, 1, AppointmentType.INDIVIDUAL));
    }

    @Test
    void book_appointmentHasConfirmedStatus() {
        service.addTimeSlot(futureSlot);
        service.login("user", "user123");
        service.book(futureSlot, 1, AppointmentType.INDIVIDUAL);
        assertEquals("Confirmed", service.getAppointments().get(0).getStatus());
    }

    @Test
    void book_violatesIndividualRule_returnsFalse() {
        service.addTimeSlot(futureSlot);
        service.login("user", "user123");
        assertFalse(service.book(futureSlot, 2, AppointmentType.INDIVIDUAL));
    }

    @Test
    void book_violatesParticipantLimit_returnsFalse() {
        service.addTimeSlot(futureSlot);
        service.login("user", "user123");
        assertFalse(service.book(futureSlot, 6, AppointmentType.GROUP));
    }

    @Test
    void book_violatesDurationRule_returnsFalse() {
        TimeSlot longSlot = new TimeSlot(
                LocalDateTime.now().plusDays(1).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(1).plusHours(3).withSecond(0).withNano(0)
        );
        service.addTimeSlot(longSlot);
        service.login("user", "user123");
        assertFalse(service.book(longSlot, 1, AppointmentType.INDIVIDUAL));
    }

    @Test
    void cancel_ownFutureAppointment_returnsTrue() {
        service.addTimeSlot(futureSlot);
        service.login("user", "user123");
        service.book(futureSlot, 1, AppointmentType.INDIVIDUAL);
        String id = service.getAppointments().get(0).getId();
        assertTrue(service.cancel(id));
    }

    @Test
    void cancel_setsStatusCancelled() {
        service.addTimeSlot(futureSlot);
        service.login("user", "user123");
        service.book(futureSlot, 1, AppointmentType.INDIVIDUAL);
        Appointment appt = service.getAppointments().get(0);
        service.cancel(appt.getId());
        assertEquals("Cancelled", appt.getStatus());
    }

    @Test
    void cancel_slotBecomesAvailableAgain() {
        service.addTimeSlot(futureSlot);
        service.login("user", "user123");
        service.book(futureSlot, 1, AppointmentType.INDIVIDUAL);
        String id = service.getAppointments().get(0).getId();
        service.cancel(id);
        assertEquals(1, service.getAvailableSlots().size());
    }

    @Test
    void cancel_nonExistentId_returnsFalse() {
        service.login("user", "user123");
        assertFalse(service.cancel("nonexistent-id"));
    }

    @Test
    void cancel_otherUsersAppointment_returnsFalse() {
        service.addTimeSlot(futureSlot);
        service.login("user", "user123");
        service.book(futureSlot, 1, AppointmentType.INDIVIDUAL);
        String id = service.getAppointments().get(0).getId();
        service.logout();

        service.registerUser("other", "pass", "other@example.com");
        service.login("other", "pass");
        assertFalse(service.cancel(id));
    }

    @Test
    void cancel_adminCancelsAnyAppointment_returnsTrue() {
        service.addTimeSlot(futureSlot);
        service.login("user", "user123");
        service.book(futureSlot, 1, AppointmentType.INDIVIDUAL);
        String id = service.getAppointments().get(0).getId();
        service.logout();

        service.login("admin", "admin123");
        assertTrue(service.cancel(id));
    }

    @Test
    void getAppointments_userSeesOnlyOwnAppointments() {
        service.addTimeSlot(futureSlot);
        service.login("user", "user123");
        service.book(futureSlot, 1, AppointmentType.INDIVIDUAL);
        List<Appointment> appts = service.getAppointments();
        assertEquals(1, appts.size());
        assertEquals("user", appts.get(0).getUserId());
    }

    @Test
    void getAppointments_adminSeesAll() {
        service.addTimeSlot(futureSlot);
        service.login("user", "user123");
        service.book(futureSlot, 1, AppointmentType.INDIVIDUAL);
        service.logout();

        service.login("admin", "admin123");
        assertEquals(1, service.getAppointments().size());
    }

    @Test
    void testReminderSent() {
        NotificationService notificationService = mock(NotificationService.class);
        SchedulingService schedulingService = new SchedulingService(notificationService);
        User user = new User("ayman", "123", "ayman@example.com", false);

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
    void reminder_notSentForCancelledAppointment() {
        NotificationService notificationService = mock(NotificationService.class);
        SchedulingService schedulingService = new SchedulingService(notificationService);
        User user = new User("ayman", "123", "ayman@example.com", false);

        Appointment appointment = new Appointment(
                "1", "ayman",
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusHours(1),
                1, AppointmentType.INDIVIDUAL
        );
        appointment.setStatus("Cancelled");

        schedulingService.checkAndSendReminders(appointment, user);
        verify(notificationService, never()).sendReminder(any(), any());
    }

    @Test
    void reminder_notSentIfAlreadySent() {
        NotificationService notificationService = mock(NotificationService.class);
        SchedulingService schedulingService = new SchedulingService(notificationService);
        User user = new User("ayman", "123", "ayman@example.com", false);

        Appointment appointment = new Appointment(
                "1", "ayman",
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusHours(1),
                1, AppointmentType.INDIVIDUAL
        );
        appointment.setReminderSent(true);

        schedulingService.checkAndSendReminders(appointment, user);
        verify(notificationService, never()).sendReminder(any(), any());
    }

    @Test
    void testSaveAndLoadAppointments() throws Exception {
        SchedulingService svc = new SchedulingService();
        svc.clearSlotsForTesting();
        assertTrue(svc.login("user", "user123"));
        TimeSlot slot = new TimeSlot(
                LocalDateTime.now().plusMinutes(10).withSecond(0).withNano(0),
                LocalDateTime.now().plusMinutes(40).withSecond(0).withNano(0)
        );
        assertTrue(svc.addTimeSlot(slot));
        assertTrue(svc.book(slot, 1, AppointmentType.INDIVIDUAL));

        Path tempFile = Files.createTempFile("appointments", ".txt");
        try {
            svc.saveAppointments(tempFile.toString());

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
        SchedulingService svc = new SchedulingService();
        svc.clearSlotsForTesting();

        TimeSlot slot = new TimeSlot(
                LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0)
        );
        assertTrue(svc.addTimeSlot(slot));

        Path tempFile = Files.createTempFile("slots", ".txt");
        try {
            svc.saveSlots(tempFile.toString());

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