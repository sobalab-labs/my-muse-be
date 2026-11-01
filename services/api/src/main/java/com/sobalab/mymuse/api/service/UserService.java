package com.sobalab.mymuse.api.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sobalab.mymuse.api.dto.ArtistDto;
import com.sobalab.mymuse.api.dto.BookmarkedConcertDto;
import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.dto.NotificationPreferencesDto;
import com.sobalab.mymuse.api.dto.UserDto;
import com.sobalab.mymuse.api.model.User;
import com.sobalab.mymuse.api.repository.UserRepository;
import com.sobalab.mymuse.api.security.AppUserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDto getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new IllegalStateException("User not authenticated");
        }
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new UserDto(
                user.getId().toString(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getName(),
                user.getPreferredLocale().name(),
                user.isOnboardingCompleted());
    }

    @Transactional
    public void deleteMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new IllegalStateException("User not authenticated");
        }
        userRepository.deleteById(principal.getUserId());
    }

    public List<ArtistDto> getFollowedArtists() {
        return Collections.emptyList();
    }

    public void followArtist(UUID artistId) {
    }

    public void unfollowArtist(UUID artistId) {
    }

    public List<BookmarkedConcertDto> getBookmarkedConcerts() {
        return Collections.emptyList();
    }

    public void bookmarkConcert(UUID concertId) {
    }

    public void unbookmarkConcert(UUID concertId) {
    }

    public List<ConcertDto> getFeed(String view, String month, String sort, String cursor, Integer limit) {
        return Collections.emptyList();
    }

    public NotificationPreferencesDto getNotificationPreferences() {
        return null;
    }

    public void updateNotificationPreferences(NotificationPreferencesDto subscription) {
    }

    public void completeOnboarding(List<UUID> followedArtistIds) {
    }
}
