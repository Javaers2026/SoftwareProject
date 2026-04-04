package com.project.observer;

import com.project.User;

public interface Observer {
    public void notify(User user, String message);   
}
