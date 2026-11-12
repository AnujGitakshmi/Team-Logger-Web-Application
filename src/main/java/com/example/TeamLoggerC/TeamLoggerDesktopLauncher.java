
package com.example.TeamLoggerC;

import javafx.application.Application;

public class TeamLoggerDesktopLauncher {

    public static void main(String[] args) {

        System.setProperty("java.awt.headless", "false");

        Application.launch(TeamLoggerDesktopApp.class, args);
    }
}
