
package com.example.TeamLoggerC;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DesktopLaunchListener {

    private static final int PORT = 8765;
    private static final int MAX_REQUEST_SIZE = 4096;

    private static final String ALLOWED_ORIGIN =
            "http://localhost:8080";

    private static final Pattern TICKET_PATTERN = Pattern.compile(
            "\\s*\\{\\s*\"ticket\"\\s*:\\s*\"([A-Za-z0-9_-]{40,50})\"\\s*}\\s*"
    );

    private final Consumer<String> launchTicketHandler;

    private HttpServer server;

    private volatile boolean dashboardNavigationPending = false;

    public DesktopLaunchListener(Consumer<String> launchTicketHandler) {

        if (launchTicketHandler == null) {
            throw new IllegalArgumentException(
                    "Launch ticket handler cannot be null."
            );
        }

        this.launchTicketHandler = launchTicketHandler;
    }

    public synchronized void start() {

        if (server != null) {
            System.out.println("Desktop Listener is already running.");
            return;
        }

        try {

            server = HttpServer.create(
                    new InetSocketAddress("127.0.0.1", PORT),
                    0
            );

            server.createContext("/health", this::handleHealth);
            server.createContext("/launch", this::handleLaunch);
            server.createContext("/navigate", this::handleNavigate);
            server.createContext(
                    "/navigation-status",
                    this::handleNavigationStatus
            );

            server.setExecutor(
                    Executors.newCachedThreadPool(runnable -> {
                        Thread thread = new Thread(runnable);
                        thread.setDaemon(true);
                        thread.setName("TeamLogger-Desktop-Listener");
                        return thread;
                    })
            );

            server.start();

            System.out.println(
                    "TeamLogger Desktop Listener started on 127.0.0.1:"
                            + PORT
            );

        } catch (IOException exception) {

            server = null;

            System.err.println(
                    "Could not start Desktop Listener: "
                            + exception.getMessage()
            );
        }
    }

    private void handleHealth(HttpExchange exchange)
            throws IOException {

        try {

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed.");
                return;
            }

            sendResponse(
                    exchange,
                    200,
                    "TeamLogger Desktop Helper is running."
            );

        } finally {
            exchange.close();
        }
    }

    private void handleLaunch(HttpExchange exchange)
            throws IOException {

        try {

            if (!checkOrigin(exchange)) {
                sendResponse(exchange, 403, "Origin not allowed.");
                return;
            }

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendPreflight(exchange);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed.");
                return;
            }

            String contentType = exchange.getRequestHeaders()
                    .getFirst("Content-Type");

            if (contentType == null ||
                    !contentType.toLowerCase().startsWith("application/json")) {

                sendResponse(exchange, 415, "JSON required.");
                return;
            }

            byte[] body = exchange.getRequestBody()
                    .readNBytes(MAX_REQUEST_SIZE + 1);

            if (body.length > MAX_REQUEST_SIZE) {
                sendResponse(exchange, 413, "Request too large.");
                return;
            }

            String requestBody = new String(
                    body,
                    StandardCharsets.UTF_8
            );

            Matcher matcher = TICKET_PATTERN.matcher(requestBody);

            if (!matcher.matches()) {
                sendResponse(exchange, 400, "Invalid launch request.");
                return;
            }

            String ticket = matcher.group(1);

            launchTicketHandler.accept(ticket);

            sendResponse(exchange, 200, "Launch request received.");

        } catch (RuntimeException exception) {

            System.err.println(
                    "Could not process launch request: "
                            + exception.getMessage()
            );

            sendResponse(exchange, 500, "Could not process launch request.");

        } finally {
            exchange.close();
        }
    }

    private void handleNavigate(HttpExchange exchange)
            throws IOException {

        try {

            if (!checkOrigin(exchange)) {
                sendResponse(exchange, 403, "Origin not allowed.");
                return;
            }

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendPreflight(exchange);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed.");
                return;
            }

            dashboardNavigationPending = true;

            System.out.println(
                    "Employee Dashboard navigation requested."
            );

            sendResponse(exchange, 200, "Navigation request accepted.");

        } finally {
            exchange.close();
        }
    }

    private void handleNavigationStatus(HttpExchange exchange)
            throws IOException {

        try {

            if (!checkOrigin(exchange)) {
                sendResponse(exchange, 403, "Origin not allowed.");
                return;
            }

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendPreflight(exchange);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed.");
                return;
            }

            boolean pending = dashboardNavigationPending;

            if (pending) {
                dashboardNavigationPending = false;
            }

            sendJsonResponse(
                    exchange,
                    200,
                    "{\"navigate\":" + pending + "}"
            );

        } finally {
            exchange.close();
        }
    }

    private boolean checkOrigin(HttpExchange exchange) {

        String origin = exchange.getRequestHeaders()
                .getFirst("Origin");

        return ALLOWED_ORIGIN.equals(origin);
    }

    private void sendPreflight(HttpExchange exchange)
            throws IOException {

        Headers headers = exchange.getResponseHeaders();

        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
        headers.set("Access-Control-Max-Age", "300");

        sendResponse(exchange, 204, "");
    }

    private void sendJsonResponse(
            HttpExchange exchange,
            int status,
            String message
    ) throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        sendResponse(exchange, status, message);
    }

    private void sendResponse(
            HttpExchange exchange,
            int status,
            String message
    ) throws IOException {

        Headers headers = exchange.getResponseHeaders();

        headers.set("Content-Type", "text/plain; charset=UTF-8");
        headers.set("Cache-Control", "no-store");
        headers.set("X-Content-Type-Options", "nosniff");

        headers.set(
                "Access-Control-Allow-Origin",
                ALLOWED_ORIGIN
        );

        headers.set("Vary", "Origin");

        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
            return;
        }

        byte[] response = message.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(status, response.length);

        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    public synchronized void stop() {

        if (server != null) {

            server.stop(0);
            server = null;

            System.out.println("Desktop Listener stopped.");
        }
    }
}
