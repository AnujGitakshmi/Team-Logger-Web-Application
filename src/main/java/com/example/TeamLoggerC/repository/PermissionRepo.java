package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepo extends JpaRepository<Permission, Integer> {

    Optional<Permission> findByName(String name);

    boolean existsByName(String name);
}

