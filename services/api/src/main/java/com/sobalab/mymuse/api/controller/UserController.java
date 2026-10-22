package com.sobalab.mymuse.api.controller;

import com.sobalab.mymuse.api.dto.*;
import com.sobalab.mymuse.api.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserDto getMe() { return userService.getMe(); }

    @DeleteMapping("/me")
    public void deleteMe() { userService.deleteMe(); }

    @GetMapping("/me/followed-artists")
    public List<ArtistDto> getFollowedArtists() { return userService.getFollowedArtists(); }

    @PostMapping("/me/followed-artists")
    public void followArtist(@RequestBody FollowArtistRequest request) { userService.followArtist(request.artistId()); }

    @DeleteMapping("/me/followed-artists/{artistId}")
    public void unfollowArtist(@PathVariable String artistId) { userService.unfollowArtist(artistId); }

    @GetMapping("/me/bookmarked-concerts")
    public List<BookmarkedConcertDto> getBookmarkedConcerts() { return userService.getBookmarkedConcerts(); }

    @PostMapping("/me/bookmarked-concerts")
    public void bookmarkConcert(@RequestBody BookmarkConcertRequest request) { userService.bookmarkConcert(request.concertId()); }

    @DeleteMapping("/me/bookmarked-concerts/{concertId}")
    public void unbookmarkConcert(@PathVariable String concertId) { userService.unbookmarkConcert(concertId); }

    @GetMapping("/me/notification-subscription")
    public NotificationSubscriptionDto getNotificationSubscription() { return userService.getNotificationSubscription(); }

    @PutMapping("/me/notification-subscription")
    public void updateNotificationSubscription(@RequestBody NotificationSubscriptionDto subscription) { userService.updateNotificationSubscription(subscription); }

    @PostMapping("/me/onboarding")
    public void completeOnboarding(@RequestBody OnboardingRequest request) { userService.completeOnboarding(request.followedArtistIds()); }

    public record FollowArtistRequest(String artistId) {}
    public record BookmarkConcertRequest(String concertId) {}
    public record OnboardingRequest(List<String> followedArtistIds) {}
}
