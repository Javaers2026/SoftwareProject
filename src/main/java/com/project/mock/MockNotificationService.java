package com.project.mock;

import com.project.service.NotificationService;
import com.project.Appointment;
import com.project.User;
import java.util.List;
import java.util.ArrayList;

public class MockNotificationService extends NotificationService{
    private List<String> sentMessages = new ArrayList<>();

    @Override
    public void sendReminder(Appointment appointment, User user){
        String message = "Mock reminder: " + appointment.getStart();
        sentMessages.add(message);
    }

    public List<String> getSentMessages(){
        return sentMessages;
    }
}
