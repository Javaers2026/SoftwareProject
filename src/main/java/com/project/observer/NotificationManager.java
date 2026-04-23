package com.project.observer;

import com.project.User;

public class NotificationManager extends Subject{

    public NotificationManager() {
        addObserver(new EmailNotification());
        addObserver(new SystemNotification());
    }
    
    public void notifyAll(User user, String message){
        notifyObservers(user, message);
    }
}
