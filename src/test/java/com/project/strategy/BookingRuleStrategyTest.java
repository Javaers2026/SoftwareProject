package com.project.strategy;

import com.project.Appointment;
import com.project.AppointmentType;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class BookingRuleStrategyTest {
    
    private static final LocalDateTime BASE = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

    private Appointment make(int durationMinutes, int participants, AppointmentType type) {
        return new Appointment("id", "user", BASE, BASE.plusMinutes(durationMinutes), participants, type);
    }

    @Test
    void durationRule_valid() {
        assertTrue(new DurationRule().isValid(make(60, 1, AppointmentType.INDIVIDUAL)));
    }

    @Test
    void durationRule_exactly120_valid() {
        assertTrue(new DurationRule().isValid(make(120, 1, AppointmentType.INDIVIDUAL)));
    }

    @Test
    void durationRule_over120_invalid() {
        assertFalse(new DurationRule().isValid(make(121, 1, AppointmentType.INDIVIDUAL)));
    }

    @Test
    void durationRule_zeroDuration_invalid() {
        assertFalse(new DurationRule().isValid(make(0, 1, AppointmentType.INDIVIDUAL)));
    }

    @Test
    void participantLimitRule_withinLimit_valid() {
        assertTrue(new ParticipantLimitRule(5).isValid(make(30, 5, AppointmentType.GROUP)));
    }

    @Test
    void participantLimitRule_overLimit_invalid() {
        assertFalse(new ParticipantLimitRule(5).isValid(make(30, 6, AppointmentType.GROUP)));
    }

    @Test
    void individualRule_oneParticipant_valid() {
        assertTrue(new IndividualRule().isValid(make(30, 1, AppointmentType.INDIVIDUAL)));
    }

    @Test
    void individualRule_multipleParticipants_invalid() {
        assertFalse(new IndividualRule().isValid(make(30, 3, AppointmentType.INDIVIDUAL)));
    }

    @Test
    void individualRule_otherType_ignored() {
        assertTrue(new IndividualRule().isValid(make(30, 5, AppointmentType.GROUP)));
    }

    @Test
    void groupRule_within10_valid() {
        assertTrue(new GroupRule().isValid(make(60, 10, AppointmentType.GROUP)));
    }

    @Test
    void groupRule_over10_invalid() {
        assertFalse(new GroupRule().isValid(make(60, 11, AppointmentType.GROUP)));
    }

    @Test
    void groupRule_otherType_ignored() {
        assertTrue(new GroupRule().isValid(make(60, 15, AppointmentType.VIRTUAL)));
    }

    @Test
    void urgentRule_within30_valid() {
        assertTrue(new UrgentRule().isValid(make(30, 1, AppointmentType.URGENT)));
    }

    @Test
    void urgentRule_over30_invalid() {
        assertFalse(new UrgentRule().isValid(make(31, 1, AppointmentType.URGENT)));
    }

    @Test
    void urgentRule_otherType_ignored() {
        assertTrue(new UrgentRule().isValid(make(90, 1, AppointmentType.ASSESSMENT)));
    }

    @Test
    void followUpRule_valid() {
        assertTrue(new FollowUpRule().isValid(make(45, 1, AppointmentType.FOLLOW_UP)));
    }

    @Test
    void followUpRule_over60_invalid() {
        assertFalse(new FollowUpRule().isValid(make(61, 1, AppointmentType.FOLLOW_UP)));
    }

    @Test
    void followUpRule_multipleParticipants_invalid() {
        assertFalse(new FollowUpRule().isValid(make(30, 2, AppointmentType.FOLLOW_UP)));
    }

    @Test
    void followUpRule_otherType_ignored() {
        assertTrue(new FollowUpRule().isValid(make(90, 3, AppointmentType.ASSESSMENT)));
    }

    @Test
    void assessmentRule_valid() {
        assertTrue(new AssessmentRule().isValid(make(60, 2, AppointmentType.ASSESSMENT)));
    }

    @Test
    void assessmentRule_over90_invalid() {
        assertFalse(new AssessmentRule().isValid(make(91, 2, AppointmentType.ASSESSMENT)));
    }

    @Test
    void assessmentRule_over3Participants_invalid() {
        assertFalse(new AssessmentRule().isValid(make(60, 4, AppointmentType.ASSESSMENT)));
    }

    @Test
    void assessmentRule_otherType_ignored() {
        assertTrue(new AssessmentRule().isValid(make(90, 5, AppointmentType.GROUP)));
    }

    @Test
    void virtualRule_valid() {
        assertTrue(new VirtualRule().isValid(make(60, 4, AppointmentType.VIRTUAL)));
    }

    @Test
    void virtualRule_oneParticipant_invalid() {
        assertFalse(new VirtualRule().isValid(make(60, 1, AppointmentType.VIRTUAL)));
    }

    @Test
    void virtualRule_over10_invalid() {
        assertFalse(new VirtualRule().isValid(make(60, 11, AppointmentType.VIRTUAL)));
    }

    @Test
    void virtualRule_otherType_ignored() {
        assertTrue(new VirtualRule().isValid(make(60, 1, AppointmentType.INDIVIDUAL)));
    }

    @Test
    void inPersonRule_valid() {
        assertTrue(new InPersonRule().isValid(make(60, 5, AppointmentType.IN_PERSON)));
    }

    @Test
    void inPersonRule_over8_invalid() {
        assertFalse(new InPersonRule().isValid(make(60, 9, AppointmentType.IN_PERSON)));
    }

    @Test
    void inPersonRule_zeroParticipants_invalid() {
        assertFalse(new InPersonRule().isValid(make(60, 0, AppointmentType.IN_PERSON)));
    }

    @Test
    void inPersonRule_otherType_ignored() {
        assertTrue(new InPersonRule().isValid(make(60, 10, AppointmentType.GROUP)));
    }
}
