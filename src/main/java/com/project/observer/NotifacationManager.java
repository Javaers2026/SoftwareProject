package com.project.observer;

import com.project.User;
import java.util.ArrayList;
import java.util.List;

public class NotifacationManager {
    
    private List<Observer> observers = new ArrayList<>();
    
    public void addObserver(Observer observer){
        observers.add(observer);
    }

    public void notifyAll(User user, String message){
        for(Observer observer : observers){
            observer.notify(user, message);
        }
    }
}
