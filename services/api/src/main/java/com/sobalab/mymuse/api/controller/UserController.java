package com.sobalab.mymuse.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sobalab.mymuse.api.dto.ArtistDto;
import com.sobalab.mymuse.api.dto.BookmarkedConcertDto;
import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.dto.CursorResponse;
import com.sobalab.mymuse.api.dto.NotificationPreferencesDto;
import com.sobalab.mymuse.api.dto.NotificationPreferencesPatchDto;
import com.sobalab.mymuse.api.dto.UserDto;
import com.sobalab.mymuse.api.model.enums.FeedSort;
import com.sobalab.mymuse.api.model.enums.FeedView;
import com.sobalab.mymuse.api.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserDto getMe() {
        return userService.getMe();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMe() {
        userService.deleteMe();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/followed-artists")
    public List<ArtistDto> getFollowedArtists() {
        return userService.getFollowedArtists();
    }

    @PostMapping("/followed-artists")
    public void followArtist(@RequestBody @Valid FollowArtistRequest request) {
        userService.followArtist(request.artistId());
    }

    @DeleteMapping("/followed-artists/{artistId}")
    public ResponseEntity<Void> unfollowArtist(@PathVariable UUID artistId) {
        userService.unfollowArtist(artistId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bookmarked-concerts")
    public List<BookmarkedConcertDto> getBookmarkedConcerts() {
        return userService.getBookmarkedConcerts();
    }

    @PostMapping("/bookmarked-concerts")
    public void bookmarkConcert(@RequestBody @Valid BookmarkConcertRequest request) {
        userService.bookmarkConcert(request.concertId());
    }

    @DeleteMapping("/bookmarked-concerts/{concertId}")
    public ResponseEntity<Void> unbookmarkConcert(@PathVariable UUID concertId) {
        userService.unbookmarkConcert(concertId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/feed")
    public CursorResponse<ConcertDto> getFeed(
            @RequestParam(required = false) FeedView view,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) FeedSort sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        List<ConcertDto> items = userService.getFeed(
                view, month, sort, cursor, limit);
        return new CursorResponse<>(items, null, false);
    }

    @GetMapping("/notification-preferences")
    public NotificationPreferencesDto getNotificationPreferences() {
        return userService.getNotificationPreferences();
    }

    @PatchMapping(path = "/notification-preferences", consumes = "application/merge-patch+json", produces = MediaType.APPLICATION_JSON_VALUE)
    public NotificationPreferencesDto patchNotificationPreferences(
            @Valid @RequestBody NotificationPreferencesPatchDto body) {
        if (body.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Patch must contain at least one field");
        }
        var current = userService.getNotificationPreferences();
        boolean email = current != null ? current.channelEmailEnabled() : false;
        boolean kakao = current != null ? current.channelKakaoEnabled() : false;
        boolean weekly = current != null ? current.generalWeeklyUpdates() : false;
        boolean artistNew = current != null ? current.artistNewConcertEnabled() : false;
        boolean ticketOpen = current != null ? current.bookmarkedEventTicketOpenEnabled() : false;

        var c = body.channels();
        var t = body.topics();

        boolean newEmail = c != null && c.email() != null ? c.email() : email;
        boolean newKakao = c != null && c.kakao() != null ? c.kakao() : kakao;
        boolean newWeekly = t != null && t.generalWeeklyUpdates() != null ? t.generalWeeklyUpdates() : weekly;
        boolean newArtistNew = t != null && t.artistNewConcert() != null ? t.artistNewConcert() : artistNew;
        boolean newTicketOpen = t != null && t.bookmarkedEventTicketOpen() != null ? t.bookmarkedEventTicketOpen()
                : ticketOpen;

        var updated = new com.sobalab.mymuse.api.dto.NotificationPreferencesDto(
                newEmail, newKakao, newWeekly, newArtistNew, newTicketOpen);
        userService.updateNotificationPreferences(updated);

        return new NotificationPreferencesDto(
                newEmail, newKakao, newWeekly, newArtistNew, newTicketOpen);
    }

    @PostMapping("/onboarding")
    public void completeOnboarding(@RequestBody @Valid OnboardingRequest request) {
        List<UUID> artistUuids = request.followedArtistIds() != null
                ? request.followedArtistIds()
                : null;
        userService.completeOnboarding(artistUuids);
    }

    public record FollowArtistRequest(@NotNull UUID artistId) {
    }

    public record BookmarkConcertRequest(@NotNull UUID concertId) {
    }

    public record OnboardingRequest(List<UUID> followedArtistIds) {
    }
}
