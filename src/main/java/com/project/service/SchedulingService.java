package com.project.service;

import com.project.*;
import com.project.observer.NotifacationManager;
import com.project.strategy.AssessmentRule;
import com.project.strategy.BookingRuleStrategy;
import com.project.strategy.DurationRule;
import com.project.strategy.FollowUpRule;
import com.project.strategy.GroupRule;
import com.project.strategy.IndividualRule;
import com.project.strategy.InPersonRule;
import com.project.strategy.ParticipantLimitRule;
import com.project.strategy.UrgentRule;
import com.project.strategy.VirtualRule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SchedulingService {
    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private List<User> users = new ArrayList<>();
    private List<Appointment> appointments = new ArrayList<>();
    private List<TimeSlot> slots = new ArrayList<>();
    private List<BookingRuleStrategy> rules = new ArrayList<>();
    private User currentUser;
    private NotificationService notificationService;

    public SchedulingService() {
        this(null);
    }

    public SchedulingService(NotificationService notificationService) {
        this.notificationService = notificationService;
        initialize();
    }

    private void initialize() {
        if (notificationService == null) {
            notificationService = new NotificationService(new NotifacationManager());
        }

        users.add(new User("admin", "admin123", true));
        users.add(new User("user", "user123", false));

        LocalDateTime base = LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0);
        for (int i = 9; i < 17; i++) {
            slots.add(new TimeSlot(base.withHour(i), base.withHour(i + 1)));
        }

        rules.add(new DurationRule());
        rules.add(new ParticipantLimitRule(5));
        rules.add(new IndividualRule());
        rules.add(new GroupRule());
        rules.add(new UrgentRule());
        rules.add(new FollowUpRule());
        rules.add(new AssessmentRule());
        rules.add(new VirtualRule());
        rules.add(new InPersonRule());
    }

    public void addRule(BookingRuleStrategy rule) {
        rules.add(rule);
    }

    public boolean login(String username, String password) {
        for (User u : users) {
            if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
                currentUser = u;
                return true;
            }
        }
        return false;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public List<TimeSlot> getAvailableSlots() {
        return slots.stream().filter(TimeSlot::isAvailable).collect(Collectors.toList());
    }

    public boolean book(TimeSlot slot, int participants, AppointmentType type) {
        if (currentUser == null) {
            return false;
        }

        Appointment appt = new Appointment(UUID.randomUUID().toString(), currentUser.getUsername(),
                slot.getStart(), slot.getEnd(), participants, type);

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
        slots.stream().filter(s -> s.getStart().equals(a.getStart()) && s.getEnd().equals(a.getEnd()))
                .forEach(s -> s.setAvailable(true));
        return true;
    }

    public List<Appointment> getAppointments() {
        if (currentUser != null && currentUser.isAdmin()) return appointments;
        return appointments.stream()
                .filter(a -> a.getUserId().equals(currentUser.getUsername()))
                .collect(Collectors.toList());
    }

    public void checkAndSendReminders(Appointment appointment, User user) {
        LocalDateTime now = LocalDateTime.now();
        if (appointment.getStart().isBefore(now.plusHours(1))) {
            notificationService.sendReminder(appointment, user);
        }
    }

    public void saveAppointments(String filename) throws IOException {
        Path path = Paths.get(filename);
        List<String> lines = new ArrayList<>();
        for (Appointment appointment : appointments) {
            lines.add(appointment.toFileString());
        }
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void loadAppointments(String filename) throws IOException {
        Path path = Paths.get(filename);
        if (!Files.exists(path)) return;

        appointments.clear();
        slots.forEach(slot -> slot.setAvailable(true));

        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            Appointment appointment = Appointment.fromFileString(line);
            appointments.add(appointment);
            updateSlotAvailability(appointment);
        }
    }

    private void updateSlotAvailability(Appointment appointment) {
        Optional<TimeSlot> existing = slots.stream()
                .filter(slot -> slot.getStart().equals(appointment.getStart()) && slot.getEnd().equals(appointment.getEnd()))
                .findFirst();

        if (existing.isPresent()) {
            if (!"Cancelled".equalsIgnoreCase(appointment.getStatus())) {
                existing.get().setAvailable(false);
            }
            return;
        }

        TimeSlot slot = new TimeSlot(appointment.getStart(), appointment.getEnd());
        slot.setAvailable("Cancelled".equalsIgnoreCase(appointment.getStatus()));
        slots.add(slot);
    }
}
