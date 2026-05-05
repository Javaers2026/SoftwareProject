package com.project;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class DomainTest {

    @Test
    void appointment_constructor_setsConfirmedAndReminderFalse() {
        Appointment a = makeAppointment();
        assertEquals("Confirmed", a.getStatus());
        assertFalse(a.isReminderSent());
    }

    @Test
    void appointment_getters_returnCorrectValues() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime end   = LocalDateTime.of(2026, 6, 1, 11, 0);
        Appointment a = new Appointment("id1", "user1", start, end, 2, AppointmentType.GROUP);

        assertEquals("id1", a.getId());
        assertEquals("user1", a.getUserId());
        assertEquals(start, a.getStart());
        assertEquals(end, a.getEnd());
        assertEquals(2, a.getParticipants());
        assertEquals(AppointmentType.GROUP, a.getType());
        assertEquals("01/06/2026 10:00", a.getFormattedStart());
        assertEquals("01/06/2026 11:00", a.getFormattedEnd());
    }

    @Test
    void appointment_setters_updateValues() {
        Appointment a = makeAppointment();
        a.setStatus("Cancelled");
        a.setReminderSent(true);
        assertEquals("Cancelled", a.getStatus());
        assertTrue(a.isReminderSent());
    }

    @Test
    void appointment_toFileString_roundTrip() {
        Appointment original = makeAppointment();
        String line = original.toFileString();
        Appointment loaded = Appointment.fromFileString(line);

        assertEquals(original.getId(), loaded.getId());
        assertEquals(original.getUserId(), loaded.getUserId());
        assertEquals(original.getStart(), loaded.getStart());
        assertEquals(original.getEnd(), loaded.getEnd());
        assertEquals(original.getParticipants(), loaded.getParticipants());
        assertEquals(original.getStatus(), loaded.getStatus());
        assertEquals(original.getType(), loaded.getType());
        assertEquals(original.isReminderSent(), loaded.isReminderSent());
    }

    @Test
    void appointment_fromFileString_withoutReminderField_defaultsFalse() {
        String line = "id1|user1|01/06/2026 10:00|01/06/2026 11:00|1|Confirmed|INDIVIDUAL";
        Appointment a = Appointment.fromFileString(line);
        assertFalse(a.isReminderSent());
        assertEquals(AppointmentType.INDIVIDUAL, a.getType());
    }

    @Test
    void appointment_fromFileString_withReminderTrue() {
        String line = "id2|user2|01/06/2026 10:00|01/06/2026 11:00|1|Confirmed|URGENT|true";
        Appointment a = Appointment.fromFileString(line);
        assertTrue(a.isReminderSent());
    }

    @Test
    void appointment_fromFileString_invalidRecord_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> Appointment.fromFileString("bad|record"));
    }

    @Test
    void appointment_fromFileString_tooManyFields_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> Appointment.fromFileString("a|b|c|d|e|f|g|h|i"));
    }

    @Test
    void timeSlot_defaultAvailableTrue() {
        TimeSlot slot = makeSlot();
        assertTrue(slot.isAvailable());
    }

    @Test
    void timeSlot_setAvailable_false() {
        TimeSlot slot = makeSlot();
        slot.setAvailable(false);
        assertFalse(slot.isAvailable());
    }

    @Test
    void timeSlot_toString_containsFormattedDates() {
        TimeSlot slot = new TimeSlot(
                LocalDateTime.of(2026, 6, 1, 10, 0),
                LocalDateTime.of(2026, 6, 1, 11, 0)
        );
        String s = slot.toString();
        assertTrue(s.contains("01/06/2026"));
        assertTrue(s.contains("10:00"));
        assertTrue(s.contains("11:00"));
    }

    @Test
    void timeSlot_getters_returnCorrectValues() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime end   = LocalDateTime.of(2026, 6, 1, 11, 0);
        TimeSlot slot = new TimeSlot(start, end);
        assertEquals(start, slot.getStart());
        assertEquals(end, slot.getEnd());
    }

    @Test
    void appointmentType_allValuesPresent() {
        AppointmentType[] types = AppointmentType.values();
        assertEquals(7, types.length);
    }

    @Test
    void user_getters_returnCorrectValues() {
        User u = new User("alice", "secret", "alice@test.com", true);
        assertEquals("alice", u.getUsername());
        assertEquals("secret", u.getPassword());
        assertEquals("alice@test.com", u.getEmail());
        assertTrue(u.isAdmin());
    }

    @Test
    void user_nonAdmin_isAdminFalse() {
        User u = new User("bob", "pass", "bob@test.com", false);
        assertFalse(u.isAdmin());
    }

    private Appointment makeAppointment() {
        return new Appointment("id1", "user1",
                LocalDateTime.of(2026, 6, 1, 10, 0),
                LocalDateTime.of(2026, 6, 1, 11, 0),
                1, AppointmentType.INDIVIDUAL);
    }

    private TimeSlot makeSlot() {
        return new TimeSlot(
                LocalDateTime.of(2026, 6, 1, 10, 0),
                LocalDateTime.of(2026, 6, 1, 11, 0)
        );
    }
}