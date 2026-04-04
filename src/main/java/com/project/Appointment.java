package com.project;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import com.project.observer.Observer;

public class Appointment {
    private String id;
    private String userId;
    private LocalDateTime start;
    private LocalDateTime end;
    private int participants;
    private String status;
    private List<Observer> observers = new ArrayList<>();

    public Appointment(String id, String userId, LocalDateTime start, LocalDateTime end, int participants) {
        this.id = id;
        this.userId = userId;
        this.start = start;
        this.end = end;
        this.participants = participants;
        this.status = "Confirmed";
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
    public int getParticipants() { return participants; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public void addObserver(Observer observer){
        observers.add(observer);
    }

    public void notifyObservers(User user, String message){
        for(Observer obs: observers){
            obs.notify(user, message);
        }
    }
}