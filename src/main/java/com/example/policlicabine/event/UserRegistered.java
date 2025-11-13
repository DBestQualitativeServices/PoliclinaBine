package com.example.policlicabine.event;

import com.example.policlicabine.entity.enums.UserRole;

import java.util.UUID;

public record UserRegistered(
    UUID userId,
    String username,
    String fullName,
    UserRole role
) {}
