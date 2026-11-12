
package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.Activity;
import com.example.TeamLoggerC.repository.ActivityRepo;
import com.example.TeamLoggerC.tracker.ActivityState;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActivityService {

    private final ActivityRepo activityRepository;

    // Stores the currently open activity record for each user.
    private final Map<Integer, Activity> currentActivities =
            new ConcurrentHashMap<>();

    public ActivityService(ActivityRepo activityRepository) {
        this.activityRepository = activityRepository;
    }

    // START ACTIVITY TRACKING
    public void startTracking(Integer userId) {

        currentActivities.computeIfAbsent(userId, id -> {

            Activity activity = new Activity(
                    id,
                    ActivityState.ACTIVE,
                    LocalDateTime.now()
            );

            activityRepository.save(activity);

            System.out.println(
                    "Activity tracking started for User: " + id
            );

            return activity;
        });
    }

    // CHANGE ACTIVITY STATE
    public void updateState(
            Integer userId,
            ActivityState newState
    ) {

        currentActivities.compute(userId, (id, currentActivity) -> {

            if (currentActivity == null) {

                Activity activity = new Activity(
                        id,
                        newState,
                        LocalDateTime.now()
                );

                activityRepository.save(activity);

                return activity;
            }

            if (currentActivity.getActivityState() == newState) {
                return currentActivity;
            }

            closeActivity(currentActivity);

            Activity newActivity = new Activity(
                    id,
                    newState,
                    LocalDateTime.now()
            );

            activityRepository.save(newActivity);

            System.out.println(
                    "User " + id + " changed state to " + newState
            );

            return newActivity;
        });
    }

    // CLOSE CURRENT ACTIVITY
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

    // STOP ACTIVITY TRACKING
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

    // GET USER ACTIVITIES
    public List<Activity> getUserActivities(Integer userId) {
        return activityRepository.findByUserId(userId);
    }

    // GET USER ACTIVITIES BY STATE
    public List<Activity> getUserActivitiesByState(
            Integer userId,
            ActivityState state
    ) {

        return activityRepository.findByUserIdAndActivityState(
                userId,
                state
        );
    }

    // GET ACTIVITIES BETWEEN TWO TIMES
    public List<Activity> getActivitiesBetween(
            Integer userId,
            LocalDateTime start,
            LocalDateTime end
    ) {

        return activityRepository.findByUserIdAndStartTimeBetween(
                userId,
                start,
                end
        );
    }

    // GET WEEKLY PRODUCTIVITY
    public List<Map<String, Object>> getWeeklyProductivity(
            Integer userId
    ) {

        List<Map<String, Object>> weeklyData = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        LocalDate today = now.toLocalDate();

        LocalDate weekStart = today.with(
                DayOfWeek.MONDAY
        );

        LocalDateTime periodStart = weekStart.atStartOfDay();

        LocalDateTime periodEnd =
                today.plusDays(1).atStartOfDay();

        List<Activity> activities =
                activityRepository.findActivitiesOverlappingPeriod(
                        userId,
                        periodStart,
                        periodEnd
                );

        DateTimeFormatter dayFormatter =
                DateTimeFormatter.ofPattern("EEE");

        for (int i = 0; i < 7; i++) {

            LocalDate date = weekStart.plusDays(i);

            LocalDateTime dayStart = date.atStartOfDay();

            LocalDateTime dayEnd =
                    date.plusDays(1).atStartOfDay();

            long activeSeconds = 0;
            long idleSeconds = 0;

            for (Activity activity : activities) {

                LocalDateTime activityStart =
                        activity.getStartTime();

                LocalDateTime activityEnd =
                        activity.getEndTime() != null
                                ? activity.getEndTime()
                                : now;

                LocalDateTime overlapStart =
                        activityStart.isAfter(dayStart)
                                ? activityStart
                                : dayStart;

                LocalDateTime overlapEnd =
                        activityEnd.isBefore(dayEnd)
                                ? activityEnd
                                : dayEnd;

                if (!overlapEnd.isAfter(overlapStart)) {
                    continue;
                }

                long duration = Duration.between(
                        overlapStart,
                        overlapEnd
                ).getSeconds();

                if (activity.getActivityState()
                        == ActivityState.ACTIVE) {

                    activeSeconds += duration;

                } else if (activity.getActivityState()
                        == ActivityState.IDLE) {

                    idleSeconds += duration;
                }
            }

            long totalSeconds = activeSeconds + idleSeconds;

            double productivity = totalSeconds > 0
                    ? (activeSeconds * 100.0) / totalSeconds
                    : 0.0;

            Map<String, Object> dayData = new HashMap<>();

            dayData.put("date", date.toString());
            dayData.put("day", date.format(dayFormatter));
            dayData.put("activeSeconds", activeSeconds);
            dayData.put("idleSeconds", idleSeconds);

            dayData.put(
                    "productivity",
                    Math.round(productivity * 10.0) / 10.0
            );

            dayData.put("hasData", totalSeconds > 0);

            weeklyData.add(dayData);
        }

        return weeklyData;
    }
}
