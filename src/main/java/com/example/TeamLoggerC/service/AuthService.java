package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.dto.LoginRequest;
import com.example.TeamLoggerC.dto.LoginResponse;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.UserRepo;
import org.springframework.stereotype.Service;
import com.example.TeamLoggerC.tracker.ActivityTrackerService;

@Service
public class AuthService {

    private final UserRepo userRepo;
    private final ActivityTrackerService activityTrackerService;

    public AuthService(UserRepo userRepo,
                       ActivityTrackerService activityTrackerService) {
        this.userRepo = userRepo;
        this.activityTrackerService = activityTrackerService;
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepo.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {

            return new LoginResponse(
                    false,
                    "Invalid email or password",
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        if (!user.getPassword().equals(request.getPassword())) {

            return new LoginResponse(
                    false,
                    "Invalid email or password",
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {

            return new LoginResponse(
                    false,
                    "Your account is inactive",
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getStatus()
            );
        }

        activityTrackerService.startTracking(user.getId());

        return new LoginResponse(
                true,
                "Login successful",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus()
        );
    }
}