package com.sobalab.mymuse.api.dto;

import com.sobalab.mymuse.api.model.User;

public record UserDto(
        String id,
        String email,
        boolean emailVerified,
        String name,
        String preferredLocale,
        boolean onboardingCompleted
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId().toString(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getName(),
                user.getPreferredLocale().getValue(),
                user.isOnboardingCompleted());
    }
}
