package com.project;

import java.time.LocalDateTime;


public class Appointment {
    private String id;
    private String userId;
    private LocalDateTime start;
    private LocalDateTime end;
    private int participants;
    private String status;
    private AppointmentType type;

    public Appointment(String id, String userId, LocalDateTime start, LocalDateTime end, int participants, AppointmentType type) {
        this.id = id;
        this.userId = userId;
        this.start = start;
        this.end = end;
        this.participants = participants;
        this.type = type;
        this.status = "Confirmed";
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
    public int getParticipants() { return participants; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public AppointmentType getType(){ return type; }

    
}