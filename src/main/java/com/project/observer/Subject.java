package com.project.observer;

import com.project.User;
import java.util.ArrayList;
import java.util.List;

public abstract class Subject {
    private List<Observer> observers = new ArrayList<>();

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public void notifyObservers(User user, String message) {
        for (Observer observer : observers) {
            observer.notify(user, message);
        }
    }
}
