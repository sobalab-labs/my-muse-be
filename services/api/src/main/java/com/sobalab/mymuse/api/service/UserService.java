package com.sobalab.mymuse.api.service;

import com.sobalab.mymuse.api.dto.*;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;

@Service
public class UserService {
    public UserDto getMe() { return null; }
    public void deleteMe() {}
    public List<ArtistDto> getFollowedArtists() { return Collections.emptyList(); }
    public void followArtist(String artistId) {}
    public void unfollowArtist(String artistId) {}
    public List<BookmarkedConcertDto> getBookmarkedConcerts() { return Collections.emptyList(); }
    public void bookmarkConcert(String concertId) {}
    public void unbookmarkConcert(String concertId) {}
    public NotificationSubscriptionDto getNotificationSubscription() { return null; }
    public void updateNotificationSubscription(NotificationSubscriptionDto subscription) {}
    public void completeOnboarding(List<String> followedArtistIds) {}
}
