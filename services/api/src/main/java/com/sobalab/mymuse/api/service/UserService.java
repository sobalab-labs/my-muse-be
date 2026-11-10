package com.sobalab.mymuse.api.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sobalab.mymuse.api.dto.ArtistDto;
import com.sobalab.mymuse.api.dto.BookmarkedConcertDto;
import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.dto.CursorResponse;
import com.sobalab.mymuse.api.dto.NotificationPreferencesDto;
import com.sobalab.mymuse.api.dto.UserDto;
import com.sobalab.mymuse.api.model.Artist;
import com.sobalab.mymuse.api.model.Concert;
import com.sobalab.mymuse.api.model.NotificationPreferences;
import com.sobalab.mymuse.api.model.User;
import com.sobalab.mymuse.api.model.UserBookmarkConcert;
import com.sobalab.mymuse.api.model.UserFollowedArtist;
import com.sobalab.mymuse.api.model.enums.FeedSort;
import com.sobalab.mymuse.api.model.enums.FeedView;
import com.sobalab.mymuse.api.repository.ArtistRepository;
import com.sobalab.mymuse.api.repository.ConcertRepository;
import com.sobalab.mymuse.api.repository.NotificationPreferencesRepository;
import com.sobalab.mymuse.api.repository.UserBookmarkConcertRepository;
import com.sobalab.mymuse.api.repository.UserFollowedArtistRepository;
import com.sobalab.mymuse.api.repository.UserRepository;
import com.sobalab.mymuse.api.security.AppUserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final ConcertRepository concertRepository;
    private final UserFollowedArtistRepository userFollowedArtistRepository;
    private final UserBookmarkConcertRepository userBookmarkConcertRepository;
    private final NotificationPreferencesRepository notificationPreferencesRepository;

    @Transactional(readOnly = true)
    public UserDto getMe() {
        User user = getCurrentUser();
        return UserDto.from(user);
    }

    @Transactional
    public void deleteMe() {
        UUID userId = getCurrentUserId();
        userRepository.deleteById(userId);
    }

    @Transactional(readOnly = true)
    public List<ArtistDto> getFollowedArtists() {
        UUID userId = getCurrentUserId();
        List<UserFollowedArtist> followed = userFollowedArtistRepository.findWithArtistsByUserId(userId);
        return followed.stream()
                .map(f -> ArtistDto.from(f.getArtist()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void followArtist(UUID artistId) {
        UUID userId = getCurrentUserId();
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Artist not found: " + artistId));
        userFollowedArtistRepository.insertFollow(userId, artist.getId());
    }

    @Transactional
    public void unfollowArtist(UUID artistId) {
        UUID userId = getCurrentUserId();
        userFollowedArtistRepository.deleteByUserIdAndArtistId(userId, artistId);
    }

    @Transactional(readOnly = true)
    public List<BookmarkedConcertDto> getBookmarkedConcerts() {
        UUID userId = getCurrentUserId();
        List<UserBookmarkConcert> bookmarks = userBookmarkConcertRepository.findByUserId(userId);
        return bookmarks.stream()
                .map(b -> new BookmarkedConcertDto(
                        b.getUser().getId(),
                        b.getConcert().getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void bookmarkConcert(UUID concertId) {
        UUID userId = getCurrentUserId();
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Concert not found: " + concertId));
        userBookmarkConcertRepository.insertIfAbsent(userId, concert.getId());
    }

    @Transactional
    public void unbookmarkConcert(UUID concertId) {
        UUID userId = getCurrentUserId();
        userBookmarkConcertRepository.deleteByUserIdAndConcertId(userId, concertId);
    }

    @Transactional(readOnly = true)
    public CursorResponse<ConcertDto> getFeed(FeedView view, String month,
            FeedSort sort, String cursor, Integer limit) {
        UUID userId = getCurrentUserId();
        int pageSize = limit == null ? 20 : limit;
        if (pageSize < 1 || pageSize > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be between 1 and 100");
        }
        if (view == FeedView.CALENDAR && (month == null || month.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Calendar view requires a month (yyyy-MM)");
        }
        UUID cursorId = null;
        Concert anchor = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                cursorId = UUID.fromString(cursor);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor must be a concert UUID");
            }
            anchor = concertRepository.findById(cursorId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Cursor concert no longer exists; restart pagination"));
        }

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        if (month != null && !month.isBlank()) {
            try {
                YearMonth ym = YearMonth.parse(month.strip());
                startDate = ym.atDay(1).atStartOfDay();
                endDate = ym.plusMonths(1).atDay(1).atStartOfDay();
            } catch (java.time.DateTimeException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must be a valid yyyy-MM value");
            }
        }

        List<Concert> concerts;
        if (sort == FeedSort.RECENTLY_ANNOUNCED) {
            concerts = concertRepository.findFollowedArtistsFeedAfterAnnouncement(userId, startDate, endDate,
                    anchor == null ? null : anchor.getCreatedAt(), cursorId, Limit.of(pageSize + 1));
        } else {
            concerts = concertRepository.findFollowedArtistsFeed(userId, startDate, endDate,
                    anchor == null ? null : anchor.getDate(), cursorId, Limit.of(pageSize + 1));
        }
        boolean hasNext = concerts.size() > pageSize;
        List<Concert> items = hasNext ? concerts.subList(0, pageSize) : concerts;
        List<ConcertDto> dtos = items.stream().map(ConcertDto::from).collect(Collectors.toList());
        String nextCursor = hasNext ? dtos.get(dtos.size() - 1).id() : null;
        return new CursorResponse<>(dtos, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public NotificationPreferencesDto getNotificationPreferences() {
        UUID userId = getCurrentUserId();
        NotificationPreferences prefs = notificationPreferencesRepository.findByUserId(userId).orElse(null);
        return NotificationPreferencesDto.from(prefs);
    }

    @Transactional
    public void updateNotificationPreferences(NotificationPreferencesDto subscription) {
        User user = getCurrentUser();
        int updated = notificationPreferencesRepository.updatePreferences(
                user.getId(),
                subscription.channelEmailEnabled(),
                subscription.channelKakaoEnabled(),
                subscription.generalWeeklyUpdates(),
                subscription.artistNewConcertEnabled(),
                subscription.bookmarkedEventTicketOpenEnabled());
        if (updated == 0) {
            NotificationPreferences prefs = NotificationPreferences.builder()
                    .user(user)
                    .channelEmailEnabled(subscription.channelEmailEnabled())
                    .channelKakaoEnabled(subscription.channelKakaoEnabled())
                    .generalWeeklyUpdates(subscription.generalWeeklyUpdates())
                    .artistNewConcertEnabled(subscription.artistNewConcertEnabled())
                    .bookmarkedEventTicketOpenEnabled(subscription.bookmarkedEventTicketOpenEnabled())
                    .build();
            notificationPreferencesRepository.save(prefs);
        }
    }

    @Transactional
    public void completeOnboarding(List<UUID> followedArtistIds) {
        UUID userId = getCurrentUserId();
        if (followedArtistIds != null) {
            for (UUID artistUuid : followedArtistIds) {
                if (artistRepository.existsById(artistUuid)) {
                    userFollowedArtistRepository.insertFollow(userId, artistUuid);
                }
            }
        }
        userRepository.completeOnboarding(userId);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new IllegalStateException("User not authenticated");
        }
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new IllegalStateException("User not authenticated");
        }
        return principal.getUserId();
    }

}
