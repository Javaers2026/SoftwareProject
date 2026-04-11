package com.project.ui;

import com.project.*;
import com.project.service.SchedulingService;
import com.project.strategy.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AppUI extends JFrame {
    private SchedulingService service;
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private DefaultTableModel appointmentModel;
    private JComboBox<String> slotPicker;
    private JComboBox<AppointmentType> typePicker;
    private JButton bookBtn;
    private JButton cancelBtn;
    private JButton addTimelineBtn;
    private JLabel roleLabel;
    private Timer reminderTimer;

    public AppUI() {
        service = new SchedulingService();
        service.addRule(new DurationRule());
        service.addRule(new ParticipantLimitRule(3));

        try {
            service.loadAppointments("appointments.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        setTitle("Appointment System");
        setSize(600, 400);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveAllData();
                dispose();
                System.exit(0);
            }
        });

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        initLoginScreen();
        initDashboard();

        add(cardPanel);
        setVisible(true);
        startReminderTimer();
    }

    private void initLoginScreen() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");

        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);
        panel.add(new JLabel(""));
        panel.add(loginBtn);
        panel.add(new JLabel(""));
        panel.add(registerBtn);

        loginBtn.addActionListener(e -> {
            if (service.login(userField.getText(), new String(passField.getPassword()))) {
                refreshData();
                cardLayout.show(cardPanel, "dashboard");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Credentials");
            }
        });

        registerBtn.addActionListener(e -> showRegistrationDialog());

        cardPanel.add(panel, "login");
    }

    private void initDashboard() {
        typePicker = new JComboBox<>(AppointmentType.values());
        JPanel panel = new JPanel(new BorderLayout());

        appointmentModel = new DefaultTableModel(new Object[]{"ID", "User", "Start", "Type", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(appointmentModel);
        table.setDefaultEditor(Object.class, null);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        roleLabel = new JLabel();
        topPanel.add(roleLabel);

        JPanel actionPanel = new JPanel();
        actionPanel.add(new JLabel("Type:"));
        actionPanel.add(typePicker);
        slotPicker = new JComboBox<>();
        bookBtn = new JButton("Book Slot");
        cancelBtn = new JButton("Cancel Selected");
        addTimelineBtn = new JButton("Add Timeline");
        JButton logoutBtn = new JButton("Logout");

        actionPanel.add(new JLabel("Available Slots:"));
        actionPanel.add(slotPicker);
        actionPanel.add(bookBtn);
        actionPanel.add(cancelBtn);
        actionPanel.add(addTimelineBtn);
        actionPanel.add(logoutBtn);

        bookBtn.addActionListener(e -> {
            int idx = slotPicker.getSelectedIndex();
            if (idx != -1) {
                TimeSlot slot = service.getAvailableSlots().get(idx);
                AppointmentType selectedType = (AppointmentType) typePicker.getSelectedItem();

                if (service.book(slot, 1, selectedType)) {
                    saveAppointments();
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Rule Violation");
                }
            }
        });

        cancelBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String id = (String) appointmentModel.getValueAt(row, 0);
                if (service.cancel(id)) {
                    saveAppointments();
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Cannot Cancel. You can only cancel your own appointments unless you are admin.");
                }
            }
        });

        addTimelineBtn.addActionListener(e -> showAddTimelineDialog());

        logoutBtn.addActionListener(e -> {
            service.logout();
            cardLayout.show(cardPanel, "login");
        });

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);
        cardPanel.add(panel, "dashboard");
    }

    private void refreshData() {
        appointmentModel.setRowCount(0);
        for (Appointment a : service.getAppointments()) {
            appointmentModel.addRow(new Object[]{a.getId(), a.getUserId(), a.getFormattedStart(), a.getType(), a.getStatus()});
        }

        slotPicker.removeAllItems();
        boolean isAdmin = service.getCurrentUser() != null && service.getCurrentUser().isAdmin();
        roleLabel.setText("Logged in as: " + service.getCurrentUser().getUsername() + (isAdmin ? " (Admin)" : ""));
        typePicker.setEnabled(!isAdmin);
        bookBtn.setEnabled(!isAdmin);
        slotPicker.setEnabled(!isAdmin);
        addTimelineBtn.setVisible(isAdmin);

        if (!isAdmin) {
            for (TimeSlot s : service.getAvailableSlots()) {
                slotPicker.addItem(s.toString());
            }
        }
    }

    private void saveAppointments() {
        try {
            service.saveAppointments("appointments.txt");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save appointments: " + e.getMessage());
        }
    }

    private void saveUsers() {
        try {
            service.saveUsers("users.txt");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save users: " + e.getMessage());
        }
    }

    private void saveSlots() {
        try {
            service.saveSlots("slots.txt");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save slots: " + e.getMessage());
        }
    }

    private void saveAllData() {
        saveAppointments();
        saveUsers();
        saveSlots();
    }

    private void showRegistrationDialog() {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        Object[] fields = {
                "New Username:", usernameField,
                "New Password:", passwordField
        };
        int option = JOptionPane.showConfirmDialog(this, fields, "Register New User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password cannot be empty.");
                return;
            }
            if (service.registerUser(username, password)) {
                JOptionPane.showMessageDialog(this, "User created successfully. You may now log in.");
            } else {
                JOptionPane.showMessageDialog(this, "Unable to register user. Username may already exist.");
            }
        }
    }

    private void showAddTimelineDialog() {
        String[] dayOptions = new String[31];
        String[] monthOptions = new String[12];
        String[] yearOptions = new String[2];
        String[] hourOptions = new String[24];
        String[] minuteOptions = new String[]{"00", "15", "30", "45"};

        for (int i = 0; i < 31; i++) {
            dayOptions[i] = String.format("%02d", i + 1);
        }
        for (int i = 0; i < 12; i++) {
            monthOptions[i] = String.format("%02d", i + 1);
        }
        int currentYear = LocalDateTime.now().getYear();
        yearOptions[0] = String.valueOf(currentYear);
        yearOptions[1] = String.valueOf(currentYear + 1);
        for (int i = 0; i < 24; i++) {
            hourOptions[i] = String.format("%02d", i);
        }

        JComboBox<String> startDay = new JComboBox<>(dayOptions);
        JComboBox<String> startMonth = new JComboBox<>(monthOptions);
        JComboBox<String> startYear = new JComboBox<>(yearOptions);
        JComboBox<String> startHour = new JComboBox<>(hourOptions);
        JComboBox<String> startMinute = new JComboBox<>(minuteOptions);

        JComboBox<String> endDay = new JComboBox<>(dayOptions);
        JComboBox<String> endMonth = new JComboBox<>(monthOptions);
        JComboBox<String> endYear = new JComboBox<>(yearOptions);
        JComboBox<String> endHour = new JComboBox<>(hourOptions);
        JComboBox<String> endMinute = new JComboBox<>(minuteOptions);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime defaultStart = now.withMinute(0).withSecond(0).withNano(0).plusHours(1);
        LocalDateTime defaultEnd = defaultStart.plusHours(1);

        String defaultStartDay = String.format("%02d", defaultStart.getDayOfMonth());
        String defaultStartMonth = String.format("%02d", defaultStart.getMonthValue());
        String defaultStartYear = String.valueOf(defaultStart.getYear());
        String defaultStartHour = String.format("%02d", defaultStart.getHour());
        String defaultStartMinute = String.format("%02d", defaultStart.getMinute());

        String defaultEndDay = String.format("%02d", defaultEnd.getDayOfMonth());
        String defaultEndMonth = String.format("%02d", defaultEnd.getMonthValue());
        String defaultEndYear = String.valueOf(defaultEnd.getYear());
        String defaultEndHour = String.format("%02d", defaultEnd.getHour());
        String defaultEndMinute = String.format("%02d", defaultEnd.getMinute());

        startDay.setSelectedItem(defaultStartDay);
        startMonth.setSelectedItem(defaultStartMonth);
        startYear.setSelectedItem(defaultStartYear);
        startHour.setSelectedItem(defaultStartHour);
        startMinute.setSelectedItem(defaultStartMinute);

        endDay.setSelectedItem(defaultEndDay);
        endMonth.setSelectedItem(defaultEndMonth);
        endYear.setSelectedItem(defaultEndYear);
        endHour.setSelectedItem(defaultEndHour);
        endMinute.setSelectedItem(defaultEndMinute);

        JPanel startPanel = new JPanel(new GridLayout(2, 5, 5, 5));
        startPanel.add(new JLabel("Day"));
        startPanel.add(new JLabel("Month"));
        startPanel.add(new JLabel("Year"));
        startPanel.add(new JLabel("Hour"));
        startPanel.add(new JLabel("Minute"));
        startPanel.add(startDay);
        startPanel.add(startMonth);
        startPanel.add(startYear);
        startPanel.add(startHour);
        startPanel.add(startMinute);

        JPanel endPanel = new JPanel(new GridLayout(2, 5, 5, 5));
        endPanel.add(new JLabel("Day"));
        endPanel.add(new JLabel("Month"));
        endPanel.add(new JLabel("Year"));
        endPanel.add(new JLabel("Hour"));
        endPanel.add(new JLabel("Minute"));
        endPanel.add(endDay);
        endPanel.add(endMonth);
        endPanel.add(endYear);
        endPanel.add(endHour);
        endPanel.add(endMinute);

        Object[] fields = {
                "Start (dd/MM/yyyy HH:mm):", startPanel,
                "End   (dd/MM/yyyy HH:mm):", endPanel
        };
        int option = JOptionPane.showConfirmDialog(this, fields, "Add New Timeline", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            try {
                String startValue = String.format("%s/%s/%s %s:%s",
                        startDay.getSelectedItem(), startMonth.getSelectedItem(), startYear.getSelectedItem(),
                        startHour.getSelectedItem(), startMinute.getSelectedItem());
                String endValue = String.format("%s/%s/%s %s:%s",
                        endDay.getSelectedItem(), endMonth.getSelectedItem(), endYear.getSelectedItem(),
                        endHour.getSelectedItem(), endMinute.getSelectedItem());
                LocalDateTime start = LocalDateTime.parse(startValue, formatter);
                LocalDateTime end = LocalDateTime.parse(endValue, formatter);
                if (!end.isAfter(start)) {
                    JOptionPane.showMessageDialog(this, "End time must be after start time.");
                    return;
                }
                if (service.addTimeSlot(new TimeSlot(start, end))) {
                    JOptionPane.showMessageDialog(this, "Timeline added successfully.");
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Unable to add timeline. It may already exist or use invalid times.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid date/time selection. Use the dropdowns in dd/MM/yyyy HH:mm format.");
            }
        }
    }

    private void startReminderTimer() {
        reminderTimer = new Timer(60_000, e -> service.checkAndSendRemindersForDueAppointments());
        reminderTimer.setInitialDelay(0);
        reminderTimer.start();
    }

    private void stopReminderTimer() {
        if (reminderTimer != null) {
            reminderTimer.stop();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AppUI::new);
    }
}