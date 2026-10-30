package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.Activity;
import com.example.TeamLoggerC.repository.ActivityRepo;
import com.example.TeamLoggerC.tracker.ActivityState;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActivityService {

    private final ActivityRepo activityRepository;

    // Stores the currently open activity for each user
    private final Map<Integer, Activity> currentActivities =
            new ConcurrentHashMap<>();

    public ActivityService(ActivityRepo activityRepository) {
        this.activityRepository = activityRepository;
    }

    // =========================================================
    // START ACTIVITY TRACKING
    // =========================================================

    public void startTracking(Integer userId) {

        if (currentActivities.containsKey(userId)) {
            return;
        }

        Activity activity = new Activity(
                userId,
                ActivityState.ACTIVE,
                LocalDateTime.now()
        );

        activityRepository.save(activity);

        currentActivities.put(userId, activity);

        System.out.println(
                "Activity tracking started for User: " + userId
        );
    }

    // =========================================================
    // CHANGE ACTIVITY STATE
    // =========================================================

    public void updateState(
            Integer userId,
            ActivityState newState
    ) {

        Activity currentActivity = currentActivities.get(userId);

        // No activity exists yet
        if (currentActivity == null) {
            startTracking(userId);
            return;
        }

        // State has not changed
        if (currentActivity.getActivityState() == newState) {
            return;
        }

        // Close previous activity
        closeActivity(currentActivity);

        // Create new activity
        Activity newActivity = new Activity(
                userId,
                newState,
                LocalDateTime.now()
        );

        activityRepository.save(newActivity);

        currentActivities.put(userId, newActivity);

        System.out.println(
                "User " + userId +
                        " changed state to " + newState
        );
    }

    // =========================================================
    // CLOSE CURRENT ACTIVITY
    // =========================================================

    private void closeActivity(Activity activity) {

        LocalDateTime endTime = LocalDateTime.now();

        activity.setEndTime(endTime);

        long durationSeconds = Duration.between(
                activity.getStartTime(),
                endTime
        ).getSeconds();

        activity.setDurationSeconds(durationSeconds);

        activityRepository.save(activity);
    }

    // =========================================================
    // STOP TRACKING
    // =========================================================

    public void stopTracking(Integer userId) {

        Activity currentActivity = currentActivities.remove(userId);

        if (currentActivity == null) {
            return;
        }

        closeActivity(currentActivity);

        System.out.println(
                "Activity tracking stopped for User: " + userId
        );
    }

    // =========================================================
    // GET USER ACTIVITIES
    // =========================================================

    public List<Activity> getUserActivities(Integer userId) {

        return activityRepository.findByUserId(userId);
    }

    // =========================================================
    // GET USER ACTIVITIES BY STATE
    // =========================================================

    public List<Activity> getUserActivitiesByState(
            Integer userId,
            ActivityState state
    ) {

        return activityRepository
                .findByUserIdAndActivityState(userId, state);
    }

    // =========================================================
    // GET ACTIVITIES BETWEEN TWO TIMES
    // =========================================================

    public List<Activity> getActivitiesBetween(
            Integer userId,
            LocalDateTime start,
            LocalDateTime end
    ) {

        return activityRepository
                .findByUserIdAndStartTimeBetween(
                        userId,
                        start,
                        end
                );
    }
}