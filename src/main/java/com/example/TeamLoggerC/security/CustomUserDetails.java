
package com.example.TeamLoggerC.security;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.service.PermissionService;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CustomUserDetails implements UserDetails {

    private final User user;
    private final PermissionService permissionService;

    public CustomUserDetails(
            User user,
            PermissionService permissionService
    ) {
        this.user = user;
        this.permissionService = permissionService;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        List<GrantedAuthority> authorities = new ArrayList<>();

        // Generate role-based authority.
        String role = user.getRole();

        if (role != null && !role.isBlank()) {

            role = role.trim().toUpperCase(Locale.ROOT);

            authorities.add(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );
        }

        // Existing employee RBAC permissions.
        if ("EMPLOYEE".equalsIgnoreCase(user.getRole())) {

            Set<String> permissions =
                    permissionService.getPermissions(user);

            if (permissions != null) {

                for (String permission : permissions) {

                    if (permission == null || permission.isBlank()) {
                        continue;
                    }

                    authorities.add(
                            new SimpleGrantedAuthority(
                                    "PERMISSION_"
                                            + permission.trim()
                                            .toUpperCase(Locale.ROOT)
                            )
                    );
                }
            }
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {

        String status = user.getStatus();

        return status != null
                && "ACTIVE".equalsIgnoreCase(status.trim());
    }
}
