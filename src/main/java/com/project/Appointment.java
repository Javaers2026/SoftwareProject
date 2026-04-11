package com.project;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Appointment {
    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String id;
    private String userId;
    private LocalDateTime start;
    private LocalDateTime end;
    private int participants;
    private String status;
    private AppointmentType type;
    private boolean reminderSent;

    public Appointment(String id, String userId, LocalDateTime start, LocalDateTime end, int participants, AppointmentType type) {
        this.id = id;
        this.userId = userId;
        this.start = start;
        this.end = end;
        this.participants = participants;
        this.type = type;
        this.status = "Confirmed";
        this.reminderSent = false;
    }

    public static Appointment fromFileString(String line) {
        String[] parts = line.split("\\|");
        if (parts.length < 7 || parts.length > 8) {
            throw new IllegalArgumentException("Invalid appointment record: " + line);
        }
        Appointment appointment = new Appointment(
                parts[0],
                parts[1],
                LocalDateTime.parse(parts[2], FILE_FORMATTER),
                LocalDateTime.parse(parts[3], FILE_FORMATTER),
                Integer.parseInt(parts[4]),
                AppointmentType.valueOf(parts[6])
        );
        appointment.setStatus(parts[5]);
        if (parts.length == 8) {
            appointment.setReminderSent(Boolean.parseBoolean(parts[7]));
        }
        return appointment;
    }

    public String toFileString() {
        return String.join("|",
                id,
                userId,
                start.format(FILE_FORMATTER),
                end.format(FILE_FORMATTER),
                String.valueOf(participants),
                status,
                type.name(),
                String.valueOf(reminderSent)
        );
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
    public int getParticipants() { return participants; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public AppointmentType getType() { return type; }
    public String getFormattedStart() { return start.format(FILE_FORMATTER); }
    public String getFormattedEnd() { return end.format(FILE_FORMATTER); }
    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }
}
