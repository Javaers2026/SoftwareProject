package com.project.observer;

import com.project.User;

import java.awt.*;

public class SystemNotification implements Observer{
     private static final String NOTIFICATION_TITLE = "Appointment Scheduler";

     @Override
    public void notify(User user, String message) {
        String fullMessage = "Hello " + user.getUsername() + "!\n" + message;

        if (!SystemTray.isSupported()) {
            System.out.println("[OS Notification - not supported, fallback] " + fullMessage);
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            Image image = Toolkit.getDefaultToolkit()
                    .createImage(new byte[0]);
            image = new java.awt.image.BufferedImage(16, 16,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);

            TrayIcon trayIcon = new TrayIcon(image, NOTIFICATION_TITLE);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);

            trayIcon.displayMessage(
                    NOTIFICATION_TITLE,
                    fullMessage,
                    TrayIcon.MessageType.INFO
            );

            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    tray.remove(trayIcon);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (AWTException e) {
            System.out.println("[OS Notification - error, fallback] " + fullMessage);
        }
    }
}
