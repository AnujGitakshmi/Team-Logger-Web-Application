 package com.example.TeamLoggerC.tracker;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseMotionListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActivityTracker implements
        NativeKeyListener,
        NativeMouseListener,
        NativeMouseMotionListener,
        NativeMouseWheelListener {

    // =====================================================
    // CONFIGURATION
    // =====================================================

    // After 10 seconds of no activity -> IDLE
    private static final long IDLE_THRESHOLD = 10_000;

    // Keyboard usage is considered active for this period
    // after the latest keyboard event.
    private static final long KEYBOARD_USAGE_THRESHOLD = 1_000;


    // =====================================================
    // ACTIVITY TIME
    // =====================================================

    private volatile long lastActivityTime =
            System.currentTimeMillis();

    private volatile long lastKeyboardActivityTime =
            0;

    private volatile long activeSeconds = 0;

    private volatile long idleSeconds = 0;

    private volatile long keyboardUsageSeconds = 0;


    // =====================================================
    // INPUT COUNTERS
    // =====================================================

    private volatile long keyboardPressCount = 0;

    private volatile long mouseClickCount = 0;


    // =====================================================
    // CURRENT STATE
    // =====================================================

    private volatile ActivityState currentState =
            ActivityState.ACTIVE;


    // =====================================================
    // SCHEDULER
    // =====================================================

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();


    // =====================================================
    // START TRACKER
    // =====================================================

    public void start() {

        Logger logger =
                Logger.getLogger(
                        GlobalScreen.class.getPackage().getName()
                );

        logger.setLevel(Level.OFF);

        try {

            GlobalScreen.registerNativeHook();

            GlobalScreen.addNativeKeyListener(this);

            GlobalScreen.addNativeMouseListener(this);

            GlobalScreen.addNativeMouseMotionListener(this);

            GlobalScreen.addNativeMouseWheelListener(this);

        } catch (NativeHookException e) {

            throw new RuntimeException(
                    "Unable to register native hook.",
                    e
            );
        }


        scheduler.scheduleAtFixedRate(
                this::calculateTime,
                1,
                1,
                TimeUnit.SECONDS
        );
    }


    // =====================================================
    // CALCULATE ACTIVE / IDLE / KEYBOARD TIME
    // =====================================================

    private void calculateTime() {

        long now =
                System.currentTimeMillis();


        // -------------------------------------------------
        // ACTIVE / IDLE
        // -------------------------------------------------

        long inactiveTime =
                now - lastActivityTime;

        if (inactiveTime >= IDLE_THRESHOLD) {

            currentState =
                    ActivityState.IDLE;

            idleSeconds++;

        } else {

            currentState =
                    ActivityState.ACTIVE;

            activeSeconds++;
        }


        // -------------------------------------------------
        // KEYBOARD USAGE
        // -------------------------------------------------

        if (lastKeyboardActivityTime > 0) {

            long keyboardInactiveTime =
                    now - lastKeyboardActivityTime;

            if (keyboardInactiveTime
                    <= KEYBOARD_USAGE_THRESHOLD) {

                keyboardUsageSeconds++;
            }
        }
    }


    // =====================================================
    // USER ACTIVITY DETECTED
    // =====================================================

    private void activityDetected() {

        lastActivityTime =
                System.currentTimeMillis();
    }


    // =====================================================
    // GETTERS
    // =====================================================

    public ActivityState getCurrentState() {

        return currentState;
    }


    public long getActiveSeconds() {

        return activeSeconds;
    }


    public long getIdleSeconds() {

        return idleSeconds;
    }


    public long getKeyboardUsageSeconds() {

        return keyboardUsageSeconds;
    }


    public long getKeyboardPressCount() {

        return keyboardPressCount;
    }


    public long getMouseClickCount() {

        return mouseClickCount;
    }


    // =====================================================
    // RESET
    // =====================================================

    public void reset() {

        activeSeconds = 0;

        idleSeconds = 0;

        keyboardUsageSeconds = 0;

        keyboardPressCount = 0;

        mouseClickCount = 0;

        lastActivityTime =
                System.currentTimeMillis();

        lastKeyboardActivityTime = 0;

        currentState =
                ActivityState.ACTIVE;
    }


    // =====================================================
    // KEYBOARD EVENTS
    // =====================================================

    @Override
    public void nativeKeyPressed(
            NativeKeyEvent e
    ) {

        activityDetected();

        lastKeyboardActivityTime =
                System.currentTimeMillis();

        keyboardPressCount++;
    }


    @Override
    public void nativeKeyReleased(
            NativeKeyEvent e
    ) {

        activityDetected();

        lastKeyboardActivityTime =
                System.currentTimeMillis();
    }


    @Override
    public void nativeKeyTyped(
            NativeKeyEvent e
    ) {

        activityDetected();

        lastKeyboardActivityTime =
                System.currentTimeMillis();
    }


    // =====================================================
    // MOUSE EVENTS
    // =====================================================

    @Override
    public void nativeMouseClicked(
            NativeMouseEvent e
    ) {

        activityDetected();

        mouseClickCount++;
    }


    @Override
    public void nativeMousePressed(
            NativeMouseEvent e
    ) {

        activityDetected();
    }


    @Override
    public void nativeMouseReleased(
            NativeMouseEvent e
    ) {

        activityDetected();
    }


    @Override
    public void nativeMouseMoved(
            NativeMouseEvent e
    ) {

        activityDetected();
    }


    @Override
    public void nativeMouseDragged(
            NativeMouseEvent e
    ) {

        activityDetected();
    }


    @Override
    public void nativeMouseWheelMoved(
            NativeMouseWheelEvent e
    ) {

        activityDetected();
    }


    // =====================================================
    // STOP TRACKER
    // =====================================================

    public void stop() {

        scheduler.shutdownNow();

        try {

            GlobalScreen.removeNativeKeyListener(
                    this
            );

            GlobalScreen.removeNativeMouseListener(
                    this
            );

            GlobalScreen.removeNativeMouseMotionListener(
                    this
            );

            GlobalScreen.removeNativeMouseWheelListener(
                    this
            );

            GlobalScreen.unregisterNativeHook();

        } catch (NativeHookException e) {

            System.err.println(
                    "Error stopping tracker: "
                            + e.getMessage()
            );
        }
    }
}

