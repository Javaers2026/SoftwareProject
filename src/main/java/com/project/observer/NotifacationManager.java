package com.project.observer;

import com.project.User;

public class NotifacationManager extends Subject{

    public NotifacationManager() {
        addObserver(new EmailNotifacation());
        addObserver(new SystemNotification());
    }
    
    public void notifyAll(User user, String message){
        notifyObservers(user, message);
    }
}
