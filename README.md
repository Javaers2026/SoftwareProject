# Appointment Scheduling System

A desktop application for managing appointments, built as a 3rd year Software Engineering project. It handles everything from booking slots to sending reminders, with separate roles for admins and regular users.

---

## What it does

Users can log in, pick an available time slot, choose an appointment type, and book it. Admins can create those time slots, see every appointment in the system, and cancel anything they need to. The app also automatically sends reminders (via email and OS system notifications) when an appointment is coming up within the hour.

There are 7 appointment types — things like Urgent, Group, Virtual, In-Person — and each one has its own set of rules. For example, a Group appointment can have at most 10 participants, an Urgent one has to be 30 minutes or less, and an Individual appointment is strictly one person. Those rules are enforced at booking time so bad data can't slip through.

Everything gets saved to plain text files (`users.txt`, `slots.txt`, `appointments.txt`) so the state persists between sessions without needing a database.

---

## Tech stack

- **Java 21** with **Swing** for the desktop GUI
- **Maven** for building and dependency management
- **JUnit 5 + Mockito** for testing
- **JaCoCo** for code coverage reports

---

## Getting started

You'll need Java 21 and Maven installed.

**Clone and build:**
```bash
git clone <repo-url>
cd SoftwareProject
mvn clean compile
```

**Run the app:**
```bash
java -cp target/classes com.project.App
```

**Default login credentials:**

| Role  | Username | Password |
|-------|----------|----------|
| Admin | admin    | admin123 |
| User  | user     | user123  |

You can also register a new user account from the login screen.

---

## Running tests

```bash
mvn test
```

To get a full coverage report:
```bash
mvn verify
```

The HTML report will be at `target/site/jacoco/index.html`. The UI layer is excluded from coverage metrics since it's mostly wiring.

---

## Project structure

```
src/
├── main/java/com/project/
│   ├── App.java                  # entry point
│   ├── Appointment.java
│   ├── TimeSlot.java
│   ├── User.java
│   ├── AppointmentType.java      # enum for all appointment types
│   ├── observer/                 # notification system (Observer pattern)
│   │   ├── NotifacationManager.java
│   │   ├── EmailNotifacation.java
│   │   └── SystemNotification.java
│   ├── strategy/                 # booking rules (Strategy pattern)
│   │   ├── BookingRuleStrategy.java
│   │   ├── DurationRule.java
│   │   ├── UrgentRule.java
│   │   ├── GroupRule.java
│   │   └── ...
│   ├── service/
│   │   ├── SchedulingService.java  # core logic
│   │   └── NotificationService.java
│   └── ui/
│       └── AppUI.java            # the whole GUI lives here
└── test/java/com/project/
    ├── DomainTest.java
    ├── service/
    ├── strategy/
    └── observer/
```

---

## Design patterns used

- **Strategy** — each appointment type's booking rules are their own class implementing a shared `BookingRuleStrategy` interface. Adding a new rule means adding a new class, nothing else changes.
- **Observer** — when a reminder fires, the `NotifacationManager` notifies all registered observers (currently email and system tray). Easy to add more channels.

---

## Data files

The three `.txt` files in the root are the "database". They're pipe-delimited and human-readable if you need to inspect or reset them. Deleting them will wipe all data — a fresh set gets created on next run.

- `users.txt` — one user per line: `username|password|isAdmin`
- `slots.txt` — `startDateTime|endDateTime|isAvailable`
- `appointments.txt` — `id|userId|start|end|participants|status|type|reminderSent`

---

## Notes

- Reminders are checked every 60 seconds in the background while the app is open. They fire if an appointment starts within the next hour and hasn't had a reminder sent yet.
- Regular users can only cancel their own appointments, and only future ones. Admins can cancel anything.
- The system tray notification requires OS support — on some setups it falls back gracefully.
