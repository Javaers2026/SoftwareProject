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

    private void saveAllData() {
        saveAppointments();
        saveUsers();
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
        JTextField startField = new JTextField();
        JTextField endField = new JTextField();
        Object[] fields = {
                "Start (dd/MM/yyyy HH:mm):", startField,
                "End   (dd/MM/yyyy HH:mm):", endField
        };
        int option = JOptionPane.showConfirmDialog(this, fields, "Add New Timeline", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            try {
                LocalDateTime start = LocalDateTime.parse(startField.getText().trim(), formatter);
                LocalDateTime end = LocalDateTime.parse(endField.getText().trim(), formatter);
                if (service.addTimeSlot(new TimeSlot(start, end))) {
                    JOptionPane.showMessageDialog(this, "Timeline added successfully.");
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Unable to add timeline. It may already exist or use invalid times.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid date/time format. Use dd/MM/yyyy HH:mm.");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AppUI::new);
    }
}