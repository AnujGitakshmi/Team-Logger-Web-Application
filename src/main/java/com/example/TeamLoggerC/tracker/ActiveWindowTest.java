
package com.example.TeamLoggerC.tracker;

public class ActiveWindowTest {

    public static void main(String[] args) {

        ActiveWindowService service =
                new ActiveWindowService();

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "      TeamLogger Active Window Test"
        );

        System.out.println(
                "=============================================="
        );

        while (true) {

            ActiveWindowService.ActiveWindowInfo info =
                    service.getActiveWindowInfo();

            System.out.println();

            System.out.println(
                    "Application : " +
                            info.getApplicationName()
            );

            System.out.println(
                    "Executable  : " +
                            info.getExecutableName()
            );

            System.out.println(
                    "Window      : " +
                            info.getWindowTitle()
            );

            System.out.println(
                    "----------------------------------------------"
            );

            try {

                Thread.sleep(2000);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                break;
            }
        }
    }
}
