package com.sobalab.mymuse.api.dto;

public record UserDto(
        String id,
        String email,
        boolean emailVerified,
        String name,
        String preferredLocale,
        boolean onboardingCompleted
) {}
