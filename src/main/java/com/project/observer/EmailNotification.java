package com.project.observer;

import com.project.User;

public class EmailNotification implements Observer{
    @Override
    public void notify(User user, String message){
        System.out.println("Email sent to "+ user.getUsername() + ": "+ message);
    }
}
