
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActivityTracker implements
        NativeKeyListener,
        NativeMouseListener,
        NativeMouseMotionListener,
        NativeMouseWheelListener {

    // =====================================================
    // GLOBAL NATIVE HOOK
    // One hook shared by all ActivityTracker instances.
    // =====================================================

    private static final Object NATIVE_HOOK_LOCK = new Object();

    private static void ensureNativeHookRegistered() {
        synchronized (NATIVE_HOOK_LOCK) {
            try {
                if (!GlobalScreen.isNativeHookRegistered()) {
                    GlobalScreen.registerNativeHook();
                    System.out.println("GLOBAL NATIVE HOOK REGISTERED");
                }
            } catch (NativeHookException e) {
                throw new RuntimeException(
                        "Unable to register global native hook.",
                        e
                );
            }
        }
    }

    /**
     * Call this only when the Spring application is shutting down.
     * Do not call this when an employee clicks Stop Work.
     */
    public static void shutdownGlobalNativeHook() {
        synchronized (NATIVE_HOOK_LOCK) {
            try {
                if (GlobalScreen.isNativeHookRegistered()) {
                    GlobalScreen.unregisterNativeHook();
                    System.out.println("GLOBAL NATIVE HOOK UNREGISTERED");
                }
            } catch (NativeHookException e) {
                System.err.println(
                        "Error shutting down global native hook: "
                                + e.getMessage()
                );
            }
        }
    }

    // =====================================================
    // CONFIGURATION
    // =====================================================

    private static final long IDLE_THRESHOLD = 10_000;
    private static final long KEYBOARD_USAGE_THRESHOLD = 1_000;

    // =====================================================
    // ACTIVITY TIME
    // =====================================================

    private volatile long lastActivityTime =
            System.currentTimeMillis();

    private volatile long lastKeyboardActivityTime = 0;

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
    // SCHEDULER AND LIFECYCLE
    // =====================================================

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    // =====================================================
    // INITIALIZE EXISTING SESSION COUNTERS
    // =====================================================

    public void initializeCounters(
            long activeSeconds,
            long idleSeconds) {

        this.activeSeconds = Math.max(0, activeSeconds);
        this.idleSeconds = Math.max(0, idleSeconds);

        this.lastActivityTime = System.currentTimeMillis();
        this.lastKeyboardActivityTime = 0;

        this.currentState = ActivityState.ACTIVE;
    }

    // =====================================================
    // START TRACKER
    // =====================================================

    public void start() {

        if (stopped.get()) {
            throw new IllegalStateException(
                    "This ActivityTracker has already been stopped. "
                            + "Create a new ActivityTracker instance."
            );
        }

        if (!started.compareAndSet(false, true)) {
            System.out.println("ActivityTracker is already running.");
            return;
        }

        Logger logger = Logger.getLogger(
                GlobalScreen.class.getPackage().getName()
        );

        logger.setLevel(Level.OFF);

        try {
            // Register the global hook only once.
            ensureNativeHookRegistered();

            // Attach this employee's listeners.
            GlobalScreen.addNativeKeyListener(this);
            GlobalScreen.addNativeMouseListener(this);
            GlobalScreen.addNativeMouseMotionListener(this);
            GlobalScreen.addNativeMouseWheelListener(this);

            // Start this employee's own counter scheduler.
            scheduler.scheduleAtFixedRate(
                    this::calculateTime,
                    1,
                    1,
                    TimeUnit.SECONDS
            );

            System.out.println(
                    "ActivityTracker started. Global hook is active."
            );

        } catch (RuntimeException e) {

            removeListeners();

            started.set(false);

            scheduler.shutdownNow();

            throw e;
        }
    }

    // =====================================================
    // CALCULATE ACTIVE / IDLE / KEYBOARD TIME
    // =====================================================

    private void calculateTime() {

        long now = System.currentTimeMillis();

        long inactiveTime = now - lastActivityTime;

        if (inactiveTime >= IDLE_THRESHOLD) {

            currentState = ActivityState.IDLE;
            idleSeconds++;

        } else {

            currentState = ActivityState.ACTIVE;
            activeSeconds++;
        }

        // Keyboard usage
        if (lastKeyboardActivityTime > 0) {

            long keyboardInactiveTime =
                    now - lastKeyboardActivityTime;

            if (keyboardInactiveTime <= KEYBOARD_USAGE_THRESHOLD) {
                keyboardUsageSeconds++;
            }
        }
    }

    // =====================================================
    // USER ACTIVITY DETECTED
    // =====================================================

    private void activityDetected() {

        lastActivityTime = System.currentTimeMillis();

        if (currentState == ActivityState.IDLE) {
            System.out.println(
                    "INPUT DETECTED - Returning to ACTIVE state"
            );
        }
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

        lastActivityTime = System.currentTimeMillis();
        lastKeyboardActivityTime = 0;

        currentState = ActivityState.ACTIVE;
    }

    // =====================================================
    // KEYBOARD EVENTS
    // =====================================================

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {

        activityDetected();

        lastKeyboardActivityTime = System.currentTimeMillis();

        keyboardPressCount++;
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {

        activityDetected();

        lastKeyboardActivityTime = System.currentTimeMillis();
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {

        activityDetected();

        lastKeyboardActivityTime = System.currentTimeMillis();
    }

    // =====================================================
    // MOUSE EVENTS
    // =====================================================

    @Override
    public void nativeMouseClicked(NativeMouseEvent e) {

        activityDetected();

        mouseClickCount++;
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        activityDetected();
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        activityDetected();
    }

    @Override
    public void nativeMouseMoved(NativeMouseEvent e) {
        activityDetected();
    }

    @Override
    public void nativeMouseDragged(NativeMouseEvent e) {
        activityDetected();
    }

    @Override
    public void nativeMouseWheelMoved(NativeMouseWheelEvent e) {
        activityDetected();
    }

    // =====================================================
    // REMOVE THIS TRACKER'S LISTENERS
    // =====================================================

    private void removeListeners() {

        GlobalScreen.removeNativeKeyListener(this);
        GlobalScreen.removeNativeMouseListener(this);
        GlobalScreen.removeNativeMouseMotionListener(this);
        GlobalScreen.removeNativeMouseWheelListener(this);
    }

    // =====================================================
    // STOP THIS EMPLOYEE'S TRACKER
    // =====================================================

    public void stop() {

        if (!started.get() || !stopped.compareAndSet(false, true)) {
            return;
        }

        // Stop this employee's scheduler.
        scheduler.shutdownNow();

        // Remove only this employee's listeners.
        removeListeners();

        // Wait for any currently executing scheduler task to finish.
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                System.err.println(
                        "ActivityTracker scheduler did not terminate in time."
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Do not unregister the global native hook here.
        System.out.println(
                "Employee ActivityTracker stopped. "
                        + "Global native hook remains registered."
        );
    }
}
