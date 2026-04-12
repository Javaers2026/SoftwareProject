package com.project.observer;

import com.project.User;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

public class ObserverTest {

    @Test
    void subject_addAndNotifyObserver() {
        NotifacationManager manager = new NotifacationManager();
        Observer mockObserver = mock(Observer.class);
        manager.addObserver(mockObserver);

        User user = new User("alice", "pass", false);
        manager.notifyAll(user, "hello");

        verify(mockObserver, times(1)).notify(user, "hello");
    }
}