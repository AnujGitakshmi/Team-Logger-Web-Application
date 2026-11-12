
package com.example.TeamLoggerC;

import jakarta.annotation.PreDestroy;
import javafx.application.Platform;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DesktopLauncherStartup {

    private final AtomicBoolean launcherStarted =
            new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void startDesktopLauncher() {

        if (!launcherStarted.compareAndSet(false, true)) {
            return;
        }

        Thread launcherThread = new Thread(() -> {

            try {

                System.out.println(
                        "Starting TeamLogger Desktop Launcher..."
                );

                TeamLoggerDesktopLauncher.main(new String[0]);

            } catch (Exception exception) {

                launcherStarted.set(false);

                System.err.println(
                        "Failed to start TeamLogger Desktop Launcher: "
                                + exception.getMessage()
                );

                exception.printStackTrace();
            }

        }, "TeamLogger-Desktop-Launcher");

        launcherThread.setDaemon(false);
        launcherThread.start();
    }

    @PreDestroy
    public void stopDesktopLauncher() {

        if (launcherStarted.get()) {

            System.out.println(
                    "Stopping TeamLogger Desktop Launcher..."
            );

            try {
                Platform.exit();

            } catch (IllegalStateException exception) {

                System.err.println(
                        "JavaFX platform is not initialized."
                );
            }
        }
    }
}
