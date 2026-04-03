package com.project.service;

import com.project.*;
import com.project.strategy.BookingRuleStrategy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class SchedulingService {
    private List<User> users = new ArrayList<>();
    private List<Appointment> appointments = new ArrayList<>();
    private List<TimeSlot> slots = new ArrayList<>();
    private List<BookingRuleStrategy> rules = new ArrayList<>();
    private User currentUser;

    public SchedulingService() {
        users.add(new User("admin", "admin123", true));
        users.add(new User("user", "user123", false));
        
        for (int i = 9; i < 17; i++) {
            slots.add(new TimeSlot(LocalDateTime.now().plusDays(1).withHour(i).withMinute(0), 
                                  LocalDateTime.now().plusDays(1).withHour(i+1).withMinute(0)));
        }
    }

    public void addRule(BookingRuleStrategy rule) { rules.add(rule); }

    public boolean login(String username, String password) {
        for (User u : users) {
            if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
                currentUser = u;
                return true;
            }
        }
        return false;
    }

    public void logout() { currentUser = null; }
    public User getCurrentUser() { return currentUser; }

    public List<TimeSlot> getAvailableSlots() {
        return slots.stream().filter(TimeSlot::isAvailable).collect(Collectors.toList());
    }

    public boolean book(TimeSlot slot, int participants) {
        Appointment appt = new Appointment(UUID.randomUUID().toString(), currentUser.getUsername(), 
                                           slot.getStart(), slot.getEnd(), participants);
        for (BookingRuleStrategy rule : rules) {
            if (!rule.isValid(appt)) return false;
        }
        appointments.add(appt);
        slot.setAvailable(false);
        return true;
    }

    public boolean cancel(String id) {
        Optional<Appointment> opt = appointments.stream().filter(a -> a.getId().equals(id)).findFirst();
        if (!opt.isPresent()) return false;
        
        Appointment a = opt.get();
        if (!currentUser.isAdmin() && !a.getUserId().equals(currentUser.getUsername())) return false;
        if (!currentUser.isAdmin() && a.getStart().isBefore(LocalDateTime.now())) return false;

        a.setStatus("Cancelled");
        slots.stream().filter(s -> s.getStart().equals(a.getStart())).forEach(s -> s.setAvailable(true));
        return true;
    }

    public List<Appointment> getAppointments() {
        if (currentUser != null && currentUser.isAdmin()) return appointments;
        return appointments.stream()
                .filter(a -> a.getUserId().equals(currentUser.getUsername()))
                .collect(Collectors.toList());
    }
}