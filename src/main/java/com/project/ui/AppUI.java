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
    private JTextField loginUserField;
    private JPasswordField loginPassField;
    private Timer reminderTimer;

    // Design System
    private final Color PRIMARY_COLOR = new Color(74, 144, 226);
    private final Color ACCENT_COLOR = new Color(92, 107, 192);
    private final Color BACKGROUND_COLOR = new Color(245, 247, 250);
    private final Color CARD_BACKGROUND = Color.WHITE;
    private final Color TEXT_COLOR = new Color(33, 37, 41);
    private final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 24);
    private final Font SUBHEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    public AppUI() {
        service = new SchedulingService();
        service.addRule(new DurationRule());
        service.addRule(new ParticipantLimitRule(3));

        try {
            service.loadAppointments("appointments.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        setTitle("Appointment Pro");
        setSize(900, 600);
        setMinimumSize(new Dimension(800, 550));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(BACKGROUND_COLOR);

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
        cardPanel.setOpaque(false);

        initLoginScreen();
        initDashboard();

        add(cardPanel);
        setLocationRelativeTo(null);
        setVisible(true);
        startReminderTimer();
    }

    private void initLoginScreen() {
        JPanel outerPanel = new JPanel(new GridBagLayout());
        outerPanel.setBackground(BACKGROUND_COLOR);

        JPanel loginCard = new JPanel(new GridBagLayout());
        loginCard.setBackground(CARD_BACKGROUND);
        loginCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(30, 40, 30, 40)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel titleLabel = new JLabel("Welcome Back", SwingConstants.CENTER);
        titleLabel.setFont(HEADER_FONT);
        titleLabel.setForeground(PRIMARY_COLOR);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        loginCard.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridy = 1;
        loginCard.add(createLabel("Username"), gbc);
        loginUserField = createTextField();
        gbc.gridx = 1;
        loginCard.add(loginUserField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        loginCard.add(createLabel("Password"), gbc);
        loginPassField = createPasswordField();
        gbc.gridx = 1;
        loginCard.add(loginPassField, gbc);

        JButton loginBtn = createPrimaryButton("Login");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 5, 5, 5);
        loginCard.add(loginBtn, gbc);

        JButton registerBtn = createSecondaryButton("Create Account");
        gbc.gridy = 4;
        gbc.insets = new Insets(5, 5, 5, 5);
        loginCard.add(registerBtn, gbc);

        loginBtn.addActionListener(e -> {
            if (service.login(loginUserField.getText(), new String(loginPassField.getPassword()))) {
                loginUserField.setText("");
                loginPassField.setText("");
                refreshData();
                cardLayout.show(cardPanel, "dashboard");
            } else {
                showError("Invalid Credentials");
            }
        });

        registerBtn.addActionListener(e -> showRegistrationDialog());

        outerPanel.add(loginCard);
        cardPanel.add(outerPanel, "login");
    }

    private void initDashboard() {
        JPanel dashboardPanel = new JPanel(new BorderLayout());
        dashboardPanel.setBackground(BACKGROUND_COLOR);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JLabel appTitle = new JLabel("Appointment Pro");
        appTitle.setFont(SUBHEADER_FONT);
        appTitle.setForeground(Color.WHITE);
        headerPanel.add(appTitle, BorderLayout.WEST);

        roleLabel = new JLabel();
        roleLabel.setFont(MAIN_FONT);
        roleLabel.setForeground(Color.WHITE);
        headerPanel.add(roleLabel, BorderLayout.CENTER);
        roleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton logoutBtn = createGhostButton("Logout");
        logoutBtn.addActionListener(e -> {
            service.logout();
            loginUserField.setText("");
            loginPassField.setText("");
            cardLayout.show(cardPanel, "login");
        });
        headerPanel.add(logoutBtn, BorderLayout.EAST);

        // Main Content
        JPanel contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Table
        appointmentModel = new DefaultTableModel(new Object[] { "ID", "User", "Start", "Type", "Status" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(appointmentModel);
        styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Side Actions
        JPanel actionPanel = new JPanel(new GridBagLayout());
        actionPanel.setBackground(CARD_BACKGROUND);
        actionPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        actionPanel.setPreferredSize(new Dimension(280, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 10, 0);

        actionPanel.add(createLabel("Appointment Type"), gbc);
        typePicker = new JComboBox<>(AppointmentType.values());
        gbc.gridy++;
        actionPanel.add(typePicker, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(10, 0, 5, 0);
        actionPanel.add(createLabel("Available Slots"), gbc);
        slotPicker = new JComboBox<>();
        gbc.gridy++;
        actionPanel.add(slotPicker, gbc);

        bookBtn = createPrimaryButton("Book Now");
        gbc.gridy++;
        gbc.insets = new Insets(20, 0, 5, 0);
        actionPanel.add(bookBtn, gbc);

        cancelBtn = createSecondaryButton("Cancel Selected");
        gbc.gridy++;
        gbc.insets = new Insets(5, 0, 5, 0);
        actionPanel.add(cancelBtn, gbc);

        addTimelineBtn = createAccentButton("Add New Timeline");
        gbc.gridy++;
        gbc.insets = new Insets(5, 0, 5, 0);
        actionPanel.add(addTimelineBtn, gbc);

        // Actions Logic
        bookBtn.addActionListener(e -> {
            int idx = slotPicker.getSelectedIndex();
            if (idx != -1) {
                TimeSlot slot = service.getAvailableSlots().get(idx);
                AppointmentType selectedType = (AppointmentType) typePicker.getSelectedItem();
                if (service.book(slot, 1, selectedType)) {
                    saveAppointments();
                    refreshData();
                    JOptionPane.showMessageDialog(this, "Success! Appointment booked.");
                } else {
                    showError("Booking failed: Rule Violation");
                }
            } else {
                showError("Please select a slot.");
            }
        });

        cancelBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String id = (String) appointmentModel.getValueAt(row, 0);
                if (service.cancel(id)) {
                    saveAppointments();
                    refreshData();
                    JOptionPane.showMessageDialog(this, "Appointment cancelled.");
                } else {
                    showError("Cannot Cancel. You can only cancel your own future appointments.");
                }
            } else {
                showError("Please select an appointment from the table.");
            }
        });

        addTimelineBtn.addActionListener(e -> showAddTimelineDialog());

        contentPanel.add(actionPanel, BorderLayout.EAST);

        dashboardPanel.add(headerPanel, BorderLayout.NORTH);
        dashboardPanel.add(contentPanel, BorderLayout.CENTER);
        cardPanel.add(dashboardPanel, "dashboard");
    }

    // --- Styled Components ---

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(MAIN_FONT);
        label.setForeground(TEXT_COLOR);
        return label;
    }

    private JTextField createTextField() {
        JTextField field = new JTextField(15);
        field.setFont(MAIN_FONT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createEmptyBorder(5, 7, 5, 7)));
        return field;
    }

    private JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField(15);
        field.setFont(MAIN_FONT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createEmptyBorder(5, 7, 5, 7)));
        return field;
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(MAIN_FONT);
        btn.setBackground(PRIMARY_COLOR);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(MAIN_FONT);
        btn.setBackground(new Color(236, 239, 241));
        btn.setForeground(TEXT_COLOR);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(207, 216, 220)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createAccentButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(MAIN_FONT);
        btn.setBackground(ACCENT_COLOR);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createGhostButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(MAIN_FONT);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void styleTable(JTable table) {
        table.setFont(MAIN_FONT);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setBackground(new Color(245, 245, 245));
        table.getTableHeader().setFont(SUBHEADER_FONT);
        table.setSelectionBackground(new Color(232, 240, 254));
        table.setSelectionForeground(TEXT_COLOR);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // --- Existing Logic Updated for New UI ---

    private void refreshData() {
        appointmentModel.setRowCount(0);
        for (Appointment a : service.getAppointments()) {
            appointmentModel.addRow(
                    new Object[] { a.getId(), a.getUserId(), a.getFormattedStart(), a.getType(), a.getStatus() });
        }

        slotPicker.removeAllItems();
        boolean isAdmin = service.getCurrentUser() != null && service.getCurrentUser().isAdmin();
        roleLabel.setText("Session: " + service.getCurrentUser().getUsername() + (isAdmin ? " [Admin]" : ""));

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
            showError("Failed to save: " + e.getMessage());
        }
    }

    private void saveUsers() {
        try {
            service.saveUsers("users.txt");
        } catch (IOException e) {
            showError("Failed to save users: " + e.getMessage());
        }
    }

    private void saveSlots() {
        try {
            service.saveSlots("slots.txt");
        } catch (IOException e) {
            showError("Failed to save slots: " + e.getMessage());
        }
    }

    private void saveAllData() {
        saveAppointments();
        saveUsers();
        saveSlots();
    }

    private void showRegistrationDialog() {
        JTextField usernameField = createTextField();
        JPasswordField passwordField = createPasswordField();
        JTextField emailField = createTextField();
        Object[] fields = {
                "New Username:", usernameField,
                "New Password:", passwordField,
                "Email Address:", emailField
        };
        int option = JOptionPane.showConfirmDialog(this, fields, "Register New User", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String email = emailField.getText().trim();
            if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                showError("All fields are required.");
                return;
            }
            if (service.registerUser(username, password, email)) {
                JOptionPane.showMessageDialog(this, "User created successfully. You may now log in.");
            } else {
                showError("Unable to register user. Username may already exist.");
            }
        }
    }

    private void showAddTimelineDialog() {
        String[] dayOptions = new String[31];
        String[] monthOptions = new String[12];
        String[] yearOptions = new String[2];
        String[] hourOptions = new String[24];
        String[] minuteOptions = new String[] { "00", "15", "30", "45" };

        for (int i = 0; i < 31; i++)
            dayOptions[i] = String.format("%02d", i + 1);
        for (int i = 0; i < 12; i++)
            monthOptions[i] = String.format("%02d", i + 1);
        int currentYear = LocalDateTime.now().getYear();
        yearOptions[0] = String.valueOf(currentYear);
        yearOptions[1] = String.valueOf(currentYear + 1);
        for (int i = 0; i < 24; i++)
            hourOptions[i] = String.format("%02d", i);

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

        startDay.setSelectedItem(String.format("%02d", defaultStart.getDayOfMonth()));
        startMonth.setSelectedItem(String.format("%02d", defaultStart.getMonthValue()));
        startYear.setSelectedItem(String.valueOf(defaultStart.getYear()));
        startHour.setSelectedItem(String.format("%02d", defaultStart.getHour()));
        startMinute.setSelectedItem(String.format("%02d", defaultStart.getMinute()));

        endDay.setSelectedItem(String.format("%02d", defaultEnd.getDayOfMonth()));
        endMonth.setSelectedItem(String.format("%02d", defaultEnd.getMonthValue()));
        endYear.setSelectedItem(String.valueOf(defaultEnd.getYear()));
        endHour.setSelectedItem(String.format("%02d", defaultEnd.getHour()));
        endMinute.setSelectedItem(String.format("%02d", defaultEnd.getMinute()));

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints dgbc = new GridBagConstraints();
        dgbc.insets = new Insets(5, 5, 5, 5);
        dgbc.fill = GridBagConstraints.HORIZONTAL;

        // Start Row
        dgbc.gridx = 0;
        dgbc.gridy = 0;
        mainPanel.add(createLabel("Start Date:"), dgbc);

        JPanel sDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        sDatePanel.add(startDay);
        sDatePanel.add(new JLabel("/"));
        sDatePanel.add(startMonth);
        sDatePanel.add(new JLabel("/"));
        sDatePanel.add(startYear);
        dgbc.gridx = 1;
        mainPanel.add(sDatePanel, dgbc);

        dgbc.gridx = 0;
        dgbc.gridy = 1;
        mainPanel.add(createLabel("Start Time:"), dgbc);

        JPanel sTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        sTimePanel.add(startHour);
        sTimePanel.add(new JLabel(":"));
        sTimePanel.add(startMinute);
        dgbc.gridx = 1;
        mainPanel.add(sTimePanel, dgbc);

        // Divider
        dgbc.gridx = 0;
        dgbc.gridy = 2;
        dgbc.gridwidth = 2;
        mainPanel.add(new JSeparator(), dgbc);
        dgbc.gridwidth = 1;

        // End Row
        dgbc.gridx = 0;
        dgbc.gridy = 3;
        mainPanel.add(createLabel("End Date:"), dgbc);

        JPanel eDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        eDatePanel.add(endDay);
        eDatePanel.add(new JLabel("/"));
        eDatePanel.add(endMonth);
        eDatePanel.add(new JLabel("/"));
        eDatePanel.add(endYear);
        dgbc.gridx = 1;
        mainPanel.add(eDatePanel, dgbc);

        dgbc.gridx = 0;
        dgbc.gridy = 4;
        mainPanel.add(createLabel("End Time:"), dgbc);

        JPanel eTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        eTimePanel.add(endHour);
        eTimePanel.add(new JLabel(":"));
        eTimePanel.add(endMinute);
        dgbc.gridx = 1;
        mainPanel.add(eTimePanel, dgbc);

        int option = JOptionPane.showConfirmDialog(this, mainPanel, "Configure New Time Slot",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
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
                    showError("End time must be after start time.");
                    return;
                }
                if (service.addTimeSlot(new TimeSlot(start, end))) {
                    JOptionPane.showMessageDialog(this, "Timeline added successfully.");
                    refreshData();
                } else {
                    showError("Unable to add timeline. It may already exist.");
                }
            } catch (Exception ex) {
                showError("Invalid date/time selection.");
            }
        }
    }

    private void startReminderTimer() {
        reminderTimer = new Timer(60_000, e -> service.checkAndSendRemindersForDueAppointments());
        reminderTimer.setInitialDelay(0);
        reminderTimer.start();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(AppUI::new);
    }
}
