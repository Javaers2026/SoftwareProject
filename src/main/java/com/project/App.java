package com.project;

import com.project.ui.AppUI;
import javax.swing.SwingUtilities;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppUI());
    }
}