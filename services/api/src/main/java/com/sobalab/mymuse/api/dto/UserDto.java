package com.sobalab.mymuse.api.dto;

public record UserDto(
        String id,
        String email,
        String name,
        String oauthProvider,
        String preferredLocale,
        boolean onboardingCompleted
) {}
