package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.UserRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    // =====================================================
    // GET ALL USERS
    // =====================================================

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }
}