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
    private static final String USER_FILE = "users.txt";
    private static final String SLOT_FILE = "slots.txt";

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
        try {
            loadUsers(USER_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            loadSlots(SLOT_FILE);
        } catch (IOException e) {
            e.printStackTrace();
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

    public boolean registerUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return false;
        }
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username)) {
                return false;
            }
        }
        users.add(new User(username, password, false));
        try {
            saveUsers(USER_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void saveUsers(String filename) throws IOException {
        Path path = Paths.get(filename);
        List<String> lines = new ArrayList<>();
        for (User user : users) {
            lines.add(String.join("|", user.getUsername(), user.getPassword(), String.valueOf(user.isAdmin())));
        }
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void loadUsers(String filename) throws IOException {
        Path path = Paths.get(filename);
        if (!Files.exists(path)) {
            return;
        }

        Map<String, User> userMap = new LinkedHashMap<>();
        userMap.put("admin", new User("admin", "admin123", true));
        userMap.put("user", new User("user", "user123", false));

        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\|");
            if (parts.length != 3) {
                continue;
            }
            userMap.put(parts[0], new User(parts[0], parts[1], Boolean.parseBoolean(parts[2])));
        }
        users = new ArrayList<>(userMap.values());
    }

    public void saveSlots(String filename) throws IOException {
        Path path = Paths.get(filename);
        List<String> lines = new ArrayList<>();
        for (TimeSlot slot : slots) {
            lines.add(String.join("|", slot.getStart().format(FILE_FORMATTER), slot.getEnd().format(FILE_FORMATTER), String.valueOf(slot.isAvailable())));
        }
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void loadSlots(String filename) throws IOException {
        Path path = Paths.get(filename);
        if (!Files.exists(path)) {
            return;
        }

        slots.clear();
        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\|");
            if (parts.length != 3) {
                continue;
            }
            TimeSlot slot = new TimeSlot(
                    LocalDateTime.parse(parts[0], FILE_FORMATTER),
                    LocalDateTime.parse(parts[1], FILE_FORMATTER)
            );
            slot.setAvailable(Boolean.parseBoolean(parts[2]));
            slots.add(slot);
        }
    }

    public boolean addTimeSlot(TimeSlot timeSlot) {
        if (timeSlot == null || timeSlot.getStart() == null || timeSlot.getEnd() == null) {
            return false;
        }
        if (!timeSlot.getStart().isBefore(timeSlot.getEnd())) {
            return false;
        }
        for (TimeSlot existing : slots) {
            if (existing.getStart().equals(timeSlot.getStart()) && existing.getEnd().equals(timeSlot.getEnd())) {
                return false;
            }
        }
        slots.add(timeSlot);
        try {
            saveSlots(SLOT_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
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

    public List<User> getAdminUsers() {
        return users.stream().filter(User::isAdmin).collect(Collectors.toList());
    }

    public User getUserByUsername(String username) {
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
    }

    public void checkAndSendReminders(Appointment appointment, User user) {
        if (appointment == null || user == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!appointment.isReminderSent() && !"Cancelled".equalsIgnoreCase(appointment.getStatus())
                && !appointment.getStart().isBefore(now)
                && appointment.getStart().isBefore(now.plusHours(1))) {
            notificationService.sendReminder(appointment, user);
            for (User admin : getAdminUsers()) {
                notificationService.sendReminder(appointment, admin);
            }
            appointment.setReminderSent(true);
        }
    }

    public void checkAndSendRemindersForDueAppointments() {
        LocalDateTime now = LocalDateTime.now();
        for (Appointment appointment : appointments) {
            if (appointment == null || appointment.isReminderSent() || "Cancelled".equalsIgnoreCase(appointment.getStatus())) {
                continue;
            }
            if (!appointment.getStart().isBefore(now.plusHours(1)) || appointment.getStart().isBefore(now)) {
                continue;
            }
            User appointmentUser = getUserByUsername(appointment.getUserId());
            checkAndSendReminders(appointment, appointmentUser);
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
