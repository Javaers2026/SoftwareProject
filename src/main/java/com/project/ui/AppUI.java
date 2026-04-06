package com.project.ui;

import com.project.*;
import com.project.service.SchedulingService;
import com.project.strategy.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AppUI extends JFrame {
    private SchedulingService service;
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private DefaultTableModel appointmentModel;
    private JComboBox<String> slotPicker;

    public AppUI() {
        service = new SchedulingService();
        service.addRule(new DurationRule());
        service.addRule(new ParticipantLimitRule(3));

        setTitle("Appointment System");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        initLoginScreen();
        initDashboard();

        add(cardPanel);
        setVisible(true);
    }

    private void initLoginScreen() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JButton loginBtn = new JButton("Login");

        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);
        panel.add(new JLabel(""));
        panel.add(loginBtn);

        loginBtn.addActionListener(e -> {
            if (service.login(userField.getText(), new String(passField.getPassword()))) {
                refreshData();
                cardLayout.show(cardPanel, "dashboard");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Credentials");
            }
        });

        cardPanel.add(panel, "login");
    }

    private void initDashboard() {
        JComboBox<AppointmentType> typePicker = new JComboBox<>(AppointmentType.values());
        JPanel panel = new JPanel(new BorderLayout());
        
        appointmentModel = new DefaultTableModel(new Object[]{"ID", "User", "Start", "Status"}, 0);
        JTable table = new JTable(appointmentModel);
        
        JPanel actionPanel = new JPanel();
        actionPanel.add(new JLabel("Type:"));
        actionPanel.add(typePicker);
        slotPicker = new JComboBox<>();
        JButton bookBtn = new JButton("Book Slot");
        JButton cancelBtn = new JButton("Cancel Selected");
        JButton logoutBtn = new JButton("Logout");

        actionPanel.add(new JLabel("Available Slots:"));
        actionPanel.add(slotPicker);
        actionPanel.add(bookBtn);
        actionPanel.add(cancelBtn);
        actionPanel.add(logoutBtn);

        bookBtn.addActionListener(e -> {
    int idx = slotPicker.getSelectedIndex();
    if (idx != -1) {
        TimeSlot slot = service.getAvailableSlots().get(idx);

        AppointmentType selectedType =
                (AppointmentType) typePicker.getSelectedItem();

        if (service.book(slot, 1, selectedType))
            refreshData();
        else
            JOptionPane.showMessageDialog(this, "Rule Violation");
    }
});

        cancelBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String id = (String) appointmentModel.getValueAt(row, 0);
                if (service.cancel(id)) refreshData();
                else JOptionPane.showMessageDialog(this, "Cannot Cancel");
            }
        });

        logoutBtn.addActionListener(e -> {
            service.logout();
            cardLayout.show(cardPanel, "login");
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);
        cardPanel.add(panel, "dashboard");
    }

    private void refreshData() {
        appointmentModel.setRowCount(0);
        for (Appointment a : service.getAppointments()) {
            appointmentModel.addRow(new Object[]{a.getId(), a.getUserId(), a.getStart(), a.getStatus()});
        }
        
        slotPicker.removeAllItems();
        for (TimeSlot s : service.getAvailableSlots()) {
            slotPicker.addItem(s.getStart().toString());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AppUI::new);
    }
}