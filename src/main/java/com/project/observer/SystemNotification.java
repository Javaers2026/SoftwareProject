package com.project.observer;

import com.project.User;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SystemNotification implements Observer {
    private static final Logger logger = Logger.getLogger(SystemNotification.class.getName());
    private static final String NOTIFICATION_TITLE = "Appointment Scheduler";
    private static TrayIcon trayIcon;

    @Override
    public void notify(User user, String message) {
        String fullMessage = "Hello " + user.getUsername() + "!\n" + message;

        if (!SystemTray.isSupported()) {
            logger.warning("[OS Notification - not supported, fallback] " + fullMessage);
            return;
        }

        try {
            TrayIcon icon = getTrayIcon();
            icon.displayMessage(NOTIFICATION_TITLE, fullMessage, TrayIcon.MessageType.INFO);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "[OS Notification - error, fallback] " + fullMessage, e);
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
