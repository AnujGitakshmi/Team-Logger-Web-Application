package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.dto.LoginRequest;
import com.example.TeamLoggerC.dto.LoginResponse;
import com.example.TeamLoggerC.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}