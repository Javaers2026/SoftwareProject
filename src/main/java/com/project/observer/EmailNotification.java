package com.project.observer;

import com.project.User;
import java.util.logging.Logger;

public class EmailNotification implements Observer{
    private static final Logger logger = Logger.getLogger(EmailNotification.class.getName());

    @Override
    public void notify(User user, String message){
        logger.info("Email sent to " + user.getEmail() + " (" + user.getUsername() + "): " + message);
    }
}
