package com.project.service;

import com.project.*;
import com.project.observer.NotificationManager;
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

/**
 * Core service layer for the Appointment Scheduling System.
 * Manages users, time slots, appointments, booking rules, and notifications.
 *
 * @author team
 * @version 1.0
 */

public class SchedulingService {

    /** Formatter used when reading and writing date/time values to files. */
    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Default filename for persisting user accounts. */
    private static final String USER_FILE = "users.txt";

    /** Default filename for persisting available time slots. */
    private static final String SLOT_FILE = "slots.txt";

    /** In-memory list of all registered users. */
    private List<User> users = new ArrayList<>();

    /** In-memory list of all appointments (confirmed and cancelled). */
    private List<Appointment> appointments = new ArrayList<>();

    /** In-memory list of all time slots (available and booked). */
    private List<TimeSlot> slots = new ArrayList<>();

    /** Chain of booking rule strategies applied when validating an appointment. */
    private List<BookingRuleStrategy> rules = new ArrayList<>();

    /** The currently authenticated user, or {@code null} if no one is logged in. */
    private User currentUser;

    /** Service responsible for sending appointment reminders via observers. */
    private NotificationService notificationService;

    /**
     * Constructs a {@code SchedulingService} with a default {@link NotificationService}.
     */
    public SchedulingService() {
        this(null);
    }

    /**
     * Constructs a {@code SchedulingService} with the given notification service.
     * If {@code null} is passed, a default service backed by {@link NotificationManager} is created.
     *
     * @param notificationService the notification service to use, or {@code null} for the default
     */
    public SchedulingService(NotificationService notificationService) {
        this.notificationService = notificationService;
        initialize();
    }

    /**
     * Initialises the service by seeding default users, loading persisted users and slots,
     * and registering all booking rule strategies.
     */
    private void initialize() {
        if (notificationService == null) {
            notificationService = new NotificationService(new NotificationManager());
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

    /**
     * Adds a custom booking rule strategy to the validation chain.
     *
     * @param rule the rule to add; must not be {@code null}
     */
    public void addRule(BookingRuleStrategy rule) {
        rules.add(rule);
    }

    /**
     * Attempts to authenticate a user with the given credentials.
     *
     * @param username the username to authenticate
     * @param password the password to verify
     * @return {@code true} if credentials match a registered user; {@code false} otherwise
     */
    public boolean login(String username, String password) {
        for (User u : users) {
            if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
                currentUser = u;
                return true;
            }
        }
        return false;
    }

    /**
     * Logs out the currently authenticated user by clearing the session.
     */
    public void logout() {
        currentUser = null;
    }

    /**
     * Returns the currently authenticated user.
     *
     * @return the current {@link User}, or {@code null} if no user is logged in
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Registers a new non-admin user with the given credentials.
     * The username must be unique (case-insensitive) and neither field may be blank.
     *
     * @param username the desired username
     * @param password the desired password
     * @return {@code true} if the user was created successfully; {@code false} if validation fails
     *         or the username already exists
     */
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

    /**
     * Persists all users to a pipe-delimited text file.
     *
     * @param filename the path of the file to write
     * @throws IOException if the file cannot be written
     */
    public void saveUsers(String filename) throws IOException {
        Path path = Paths.get(filename);
        List<String> lines = new ArrayList<>();
        for (User user : users) {
            lines.add(String.join("|", user.getUsername(), user.getPassword(), String.valueOf(user.isAdmin())));
        }
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Loads users from a pipe-delimited text file, merging with the default admin and user accounts.
     * Lines that are malformed (not exactly 3 fields) are silently skipped.
     *
     * @param filename the path of the file to read
     * @throws IOException if the file exists but cannot be read
     */
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

    /**
     * Persists all time slots to a pipe-delimited text file.
     *
     * @param filename the path of the file to write
     * @throws IOException if the file cannot be written
     */
    public void saveSlots(String filename) throws IOException {
        Path path = Paths.get(filename);
        List<String> lines = new ArrayList<>();
        for (TimeSlot slot : slots) {
            lines.add(String.join("|", slot.getStart().format(FILE_FORMATTER), slot.getEnd().format(FILE_FORMATTER), String.valueOf(slot.isAvailable())));
        }
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Loads time slots from a pipe-delimited text file, replacing any slots currently in memory.
     * Lines that are malformed (not exactly 3 fields) are silently skipped.
     *
     * @param filename the path of the file to read
     * @throws IOException if the file exists but cannot be read
     */
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

    /**
     * Adds a new time slot to the schedule if it is valid and not a duplicate.
     * A valid slot must have a non-null start and end, and the start must be before the end.
     *
     * @param timeSlot the slot to add
     * @return {@code true} if the slot was added; {@code false} if it is invalid or already exists
     */
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

    /**
     * Returns all time slots that are currently available for booking.
     *
     * @return a list of available {@link TimeSlot} objects; never {@code null}
     */
    public List<TimeSlot> getAvailableSlots() {
        return slots.stream().filter(TimeSlot::isAvailable).collect(Collectors.toList());
    }

    /**
     * Books the given time slot for the currently logged-in user.
     * All registered booking rules are validated before the appointment is confirmed.
     * Requires an authenticated user; returns {@code false} if no user is logged in.
     *
     * @param slot         the time slot to book
     * @param participants the number of participants for the appointment
     * @param type         the type of appointment
     * @return {@code true} if the appointment was booked successfully; {@code false} if
     *         no user is logged in or any booking rule is violated
     */
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

    /**
     * Cancels the appointment with the given ID.
     * Regular users may only cancel their own future appointments.
     * Administrators may cancel any appointment regardless of owner or time.
     *
     * @param id the unique identifier of the appointment to cancel
     * @return {@code true} if the appointment was cancelled; {@code false} if it does not exist,
     *         the current user lacks permission, or the appointment is already in the past
     */
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

    /**
     * Returns the list of appointments visible to the current user.
     * Administrators see all appointments; regular users see only their own.
     *
     * @return a list of {@link Appointment} objects; never {@code null}
     */
    public List<Appointment> getAppointments() {
        if (currentUser != null && currentUser.isAdmin()) return appointments;
        return appointments.stream()
                .filter(a -> a.getUserId().equals(currentUser.getUsername()))
                .collect(Collectors.toList());
    }

    /**
     * Returns all users with administrator privileges.
     *
     * @return a list of admin {@link User} objects; never {@code null}
     */
    public List<User> getAdminUsers() {
        return users.stream().filter(User::isAdmin).collect(Collectors.toList());
    }

    /**
     * Looks up a user by their username.
     *
     * @param username the username to search for
     * @return the matching {@link User}, or {@code null} if no user is found
     */
    public User getUserByUsername(String username) {
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
    }

    /**
     * Sends a reminder for a single appointment to the given user and all administrators,
     * provided the appointment is not cancelled, the reminder has not already been sent,
     * and the appointment starts within the next hour.
     *
     * @param appointment the appointment to check; must not be {@code null}
     * @param user        the user to notify; must not be {@code null}
     */
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

    /**
     * Iterates over all appointments and sends reminders for any that are due within the next hour,
     * have not yet been reminded, and are not cancelled.
     */
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

    /**
     * Persists all appointments to a pipe-delimited text file.
     *
     * @param filename the path of the file to write
     * @throws IOException if the file cannot be written
     */
    public void saveAppointments(String filename) throws IOException {
        Path path = Paths.get(filename);
        List<String> lines = new ArrayList<>();
        for (Appointment appointment : appointments) {
            lines.add(appointment.toFileString());
        }
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Loads appointments from a pipe-delimited text file, replacing all appointments currently
     * in memory and resetting slot availability accordingly.
     *
     * @param filename the path of the file to read
     * @throws IOException if the file exists but cannot be read
     */
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

    /**
     * Updates the availability of the slot matching the given appointment.
     * If no matching slot exists, a new one is created and added to the slot list.
     *
     * @param appointment the appointment whose slot availability should be updated
     */
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

    /**
     * Clears all time slots from memory. Used only in tests to ensure a clean state.
     */
    void clearSlotsForTesting() {
    slots.clear();
}

}
