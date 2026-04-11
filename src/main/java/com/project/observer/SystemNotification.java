package com.project.observer;

import com.project.User;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SystemNotification implements Observer {
    private static final String NOTIFICATION_TITLE = "Appointment Scheduler";
    private static TrayIcon trayIcon;

    @Override
    public void notify(User user, String message) {
        String fullMessage = "Hello " + user.getUsername() + "!\n" + message;

        if (!SystemTray.isSupported()) {
            System.out.println("[OS Notification - not supported, fallback] " + fullMessage);
            return;
        }

        try {
            TrayIcon icon = getTrayIcon();
            icon.displayMessage(NOTIFICATION_TITLE, fullMessage, TrayIcon.MessageType.INFO);
        } catch (Throwable e) {
            System.out.println("[OS Notification - error, fallback] " + fullMessage);
        }
    }

    private synchronized TrayIcon getTrayIcon() throws AWTException {
        if (trayIcon == null) {
            SystemTray tray = SystemTray.getSystemTray();
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            trayIcon = new TrayIcon(image, NOTIFICATION_TITLE);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
        }
        return trayIcon;
    }
}
