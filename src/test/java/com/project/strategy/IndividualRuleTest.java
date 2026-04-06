package com.project.strategy;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.project.Appointment;
import com.project.AppointmentType;

public class IndividualRuleTest {
    @Test
void testIndividualRuleFails() {
    Appointment appt = new Appointment(
        "1",
        "user",
        LocalDateTime.now(),
        LocalDateTime.now().plusHours(1),
        3,
        AppointmentType.INDIVIDUAL
    );

    IndividualRule rule = new IndividualRule();

    assertFalse(rule.isValid(appt));
}
}
