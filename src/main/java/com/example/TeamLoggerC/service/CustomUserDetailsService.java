 package com.example.TeamLoggerC.security;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.service.PermissionService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepo userRepo;
    private final PermissionService permissionService;

    public CustomUserDetailsService(
            UserRepo userRepo,
            PermissionService permissionService
    ) {
        this.userRepo = userRepo;
        this.permissionService = permissionService;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with email: " + email
                        )
                );

        return new CustomUserDetails(
                user,
                permissionService
        );
    }
}

