
package com.example.TeamLoggerC;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.awt.Desktop;
import java.net.URI;

public class TeamLoggerDesktopApp extends Application {

    private static final String BOOTSTRAP_URL =
            "http://localhost:8080/desktop/bootstrap";

    private static final String WORKSTATION_PATH =
            "/workstation";

    private DesktopLaunchListener launchListener;

    private Stage workstationStage;

    private WebView webView;

    private WebEngine webEngine;

    private boolean launchInProgress = false;

    /*
     * Temporary protection:
     * Once the employee closes the Workstation, another launch
     * is ignored until the helper is restarted.
     */
    private boolean workstationClosed = false;

    @Override
    public void start(Stage primaryStage) {

        Platform.setImplicitExit(false);

        launchListener = new DesktopLaunchListener(
                this::handleLaunchTicket
        );

        launchListener.start();

        System.out.println("TeamLogger Desktop Helper started.");
        System.out.println("Waiting for employee login...");
    }

    private void handleLaunchTicket(String ticket) {

        Platform.runLater(() -> {

            System.out.println(
                    "Launch ticket received by JavaFX application."
            );

            if (ticket == null ||
                    !ticket.matches("[A-Za-z0-9_-]{40,50}")) {

                System.err.println("Invalid desktop launch ticket.");
                return;
            }

            if (workstationClosed) {

                System.out.println(
                        "Workstation was already closed. "
                                + "Ignoring another launch request."
                );

                return;
            }

            if (launchInProgress || workstationStage != null) {

                System.out.println(
                        "Workstation is already opening or running."
                );

                return;
            }

            openWorkstation(ticket);
        });
    }

    private void openWorkstation(String ticket) {

        launchInProgress = true;

        try {

            workstationStage = new Stage();

            workstationStage.setTitle(
                    "TeamLogger - Personal Workstation"
            );

            // Standard TeamLogger window size
            workstationStage.setWidth(400);
            workstationStage.setHeight(750);

            workstationStage.setMinWidth(500);
            workstationStage.setMinHeight(600);

            webView = new WebView();

            webView.setContextMenuEnabled(false);

            webEngine = webView.getEngine();

            webEngine.setJavaScriptEnabled(true);

            Scene scene = new Scene(webView);

            workstationStage.setScene(scene);

            workstationStage.setOnCloseRequest(event -> {

                workstationClosed = true;
                launchInProgress = false;

                workstationStage = null;
                webView = null;
                webEngine = null;

                System.out.println(
                        "Workstation closed by employee."
                );

                System.out.println(
                        "Desktop helper remains running."
                );
            });

            /*
             * Observe page loading.
             *
             * When bootstrap loads successfully, pass the ticket
             * directly to its JavaScript function.
             *
             * The ticket is never placed in the URL.
             */
            webEngine.getLoadWorker()
                    .stateProperty()
                    .addListener((observable, oldState, newState) -> {

                        if (newState == Worker.State.SUCCEEDED) {

                            String currentLocation =
                                    webEngine.getLocation();

                            System.out.println(
                                    "WebView loaded: " + currentLocation
                            );

                            if (currentLocation != null &&
                                    currentLocation.endsWith(
                                            "/desktop/bootstrap"
                                    )) {

                                deliverTicketToBootstrap(ticket);
                            }

                            else if (currentLocation != null &&
                                    currentLocation.endsWith(
                                            WORKSTATION_PATH
                                    )) {

                                launchInProgress = false;

                                /*
                                 * Install the dashboard link handler
                                 * after the Workstation page is loaded.
                                 */
                                installDashboardLinkHandler();

                                workstationStage.show();
                                workstationStage.toFront();

                                System.out.println(
                                        "Employee Workstation opened successfully."
                                );
                            }
                        }

                        else if (newState == Worker.State.FAILED) {

                            launchInProgress = false;

                            System.err.println(
                                    "Could not load Workstation page."
                            );

                            if (workstationStage != null) {
                                workstationStage.show();
                            }
                        }
                    });

            /*
             * Show the window while authentication is taking place.
             */
            workstationStage.show();

            /*
             * Load the bootstrap page.
             */
            webEngine.load(BOOTSTRAP_URL);

        } catch (Exception exception) {

            launchInProgress = false;

            System.err.println(
                    "Could not open Workstation: "
                            + exception.getMessage()
            );

            exception.printStackTrace();

            closeWorkstationStage();
        }
    }

    private void deliverTicketToBootstrap(String ticket) {

        if (webEngine == null) {
            return;
        }

        try {

            /*
             * Ticket contains only URL-safe Base64 characters.
             */
            String javascript =
                    "receiveLaunchTicket('" + ticket + "');";

            webEngine.executeScript(javascript);

            System.out.println(
                    "Launch ticket delivered to bootstrap page."
            );

        } catch (Exception exception) {

            launchInProgress = false;

            System.err.println(
                    "Could not deliver launch ticket: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    /*
     * Intercept the Employee Dashboard link before WebView
     * navigates away from the Workstation.
     */
    private void installDashboardLinkHandler() {

        try {

            JSObject window = (JSObject)
                    webEngine.executeScript("window");

            window.setMember(
                    "desktopBridge",
                    new DesktopBridge()
            );

            String javascript = """
                    document.addEventListener("click", function(event) {

                        const link = event.target.closest("a");

                        if (!link || !link.href) {
                            return;
                        }

                        const url = new URL(link.href);

                        const isEmployeeDashboard =
                            url.protocol === "http:" &&
                            url.hostname === "localhost" &&
                            url.port === "8080" &&
                            url.pathname === "/employee/dashboard";

                        if (isEmployeeDashboard) {

                            event.preventDefault();
                            event.stopPropagation();
                            event.stopImmediatePropagation();

                            window.desktopBridge.openDashboard(link.href);
                        }

                    }, true);
                    """;

            webEngine.executeScript(javascript);

            System.out.println(
                    "Employee Dashboard link interception installed."
            );

        } catch (Exception exception) {

            System.err.println(
                    "Could not install dashboard link handler: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    /*
     * JavaScript-to-Java bridge.
     *
     * Called when the employee clicks the dashboard link.
     */
    public final class DesktopBridge {

        public void openDashboard(String dashboardUrl) {

            Platform.runLater(() -> {

                try {

                    URI uri = URI.create(dashboardUrl);

                    boolean validDashboard =
                            "http".equalsIgnoreCase(uri.getScheme())
                                    && "localhost".equalsIgnoreCase(uri.getHost())
                                    && uri.getPort() == 8080
                                    && "/employee/dashboard".equals(uri.getPath());

                    if (!validDashboard) {

                        System.err.println(
                                "Invalid Employee Dashboard URL."
                        );

                        return;
                    }

                    if (!Desktop.isDesktopSupported()
                            || !Desktop.getDesktop().isSupported(
                            Desktop.Action.BROWSE)) {

                        System.err.println(
                                "System browser is not supported."
                        );

                        return;
                    }

                    Desktop.getDesktop().browse(uri);

                    System.out.println(
                            "Employee Dashboard opened in the default browser."
                    );

                } catch (Exception exception) {

                    System.err.println(
                            "Could not open Employee Dashboard: "
                                    + exception.getMessage()
                    );

                    exception.printStackTrace();
                }
            });
        }
    }

    private void closeWorkstationStage() {

        if (workstationStage != null) {

            workstationStage.close();

            workstationStage = null;
        }

        webView = null;
        webEngine = null;
    }

    @Override
    public void stop() {

        if (launchListener != null) {
            launchListener.stop();
        }

        System.out.println(
                "TeamLogger Desktop Helper stopped."
        );
    }
}
