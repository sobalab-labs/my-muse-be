package com.sobalab.mymuse.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.sobalab.mymuse.api.dto.NotificationPreferencesDto;
import com.sobalab.mymuse.api.dto.UserDto;
import com.sobalab.mymuse.api.model.Artist;
import com.sobalab.mymuse.api.model.Concert;
import com.sobalab.mymuse.api.model.NotificationPreferences;
import com.sobalab.mymuse.api.model.User;
import com.sobalab.mymuse.api.model.UserLikeConcert;
import com.sobalab.mymuse.api.model.enums.FeedSort;
import com.sobalab.mymuse.api.model.enums.FeedView;
import com.sobalab.mymuse.api.repository.ArtistRepository;
import com.sobalab.mymuse.api.repository.ConcertRepository;
import com.sobalab.mymuse.api.repository.NotificationPreferencesRepository;
import com.sobalab.mymuse.api.repository.UserFollowedArtistRepository;
import com.sobalab.mymuse.api.repository.UserLikeConcertRepository;
import com.sobalab.mymuse.api.repository.UserRepository;
import com.sobalab.mymuse.api.security.AppUserPrincipal;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private ConcertRepository concertRepository;
    @Mock
    private UserFollowedArtistRepository userFollowedArtistRepository;
    @Mock
    private UserLikeConcertRepository userLikeConcertRepository;
    @Mock
    private NotificationPreferencesRepository notificationPreferencesRepository;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User user;
    private AppUserPrincipal principal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .build();
        // Simulate the ID assigned by JPA before a persisted user becomes a principal.
        ReflectionTestUtils.setField(user, "id", userId);
        principal = new AppUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void followedFeedUsesDateCursor() {
        var id = UUID.randomUUID();
        var anchor = mock(Concert.class);
        var date = java.time.LocalDateTime.of(2030, 1, 1, 0, 0);
        when(anchor.getDate()).thenReturn(date);
        when(concertRepository.findById(id)).thenReturn(Optional.of(anchor));
        when(concertRepository.findFollowedArtistsFeed(any(), any(), any(), any(), any(), any()))
                .thenReturn(java.util.List.of());
        userService.getFeed(FeedView.LIST, null, FeedSort.DDAY, id.toString(), 10);
        verify(concertRepository).findFollowedArtistsFeed(org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(date), org.mockito.ArgumentMatchers.eq(id),
                org.mockito.ArgumentMatchers.argThat(limit -> limit.max() == 10));
    }

    @Test
    void followedFeedUsesAnnouncementCursor() {
        var id = UUID.randomUUID();
        var anchor = mock(Concert.class);
        var created = java.time.Instant.parse("2029-01-01T00:00:00Z");
        when(anchor.getCreatedAt()).thenReturn(created);
        when(concertRepository.findById(id)).thenReturn(Optional.of(anchor));
        when(concertRepository.findFollowedArtistsFeedAfterAnnouncement(any(), any(), any(), any(), any(), any()))
                .thenReturn(java.util.List.of());
        userService.getFeed(FeedView.LIST, null, FeedSort.RECENTLY_ANNOUNCED, id.toString(), 10);
        verify(concertRepository).findFollowedArtistsFeedAfterAnnouncement(org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(created), org.mockito.ArgumentMatchers.eq(id),
                org.mockito.ArgumentMatchers.argThat(limit -> limit.max() == 10));
    }

    @Test
    void followedFeedRejectsMalformedAndDeletedCursors() {
        assertThatThrownBy(() -> userService.getFeed(FeedView.LIST, null, FeedSort.DDAY, "invalid", 10))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var id = UUID.randomUUID();
        when(concertRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getFeed(FeedView.LIST, null, FeedSort.DDAY, id.toString(), 10))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void getMe_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDto dto = userService.getMe();

        assertThat(dto).isNotNull();
        assertThat(dto.email()).isEqualTo("test@example.com");
        assertThat(dto.name()).isEqualTo("Test User");
    }

    @Test
    void getMe_NotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> userService.getMe())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not authenticated");
    }

    @Test
    void getMe_UserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMe())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void deleteMe_Success() {
        userService.deleteMe();
        verify(userRepository).deleteById(userId);
    }

    @Test
    void followArtist_Success() {
        UUID artistId = UUID.randomUUID();
        Artist artist = mock(Artist.class);
        when(artist.getId()).thenReturn(artistId);
        when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

        userService.followArtist(artistId);

        verify(userFollowedArtistRepository).insertFollow(userId, artistId);
    }

    @Test
    void followArtist_ArtistNotFound() {
        UUID artistId = UUID.randomUUID();
        when(artistRepository.findById(artistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.followArtist(artistId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Artist not found");
    }

    @Test
    void unfollowArtist_Success() {
        UUID artistId = UUID.randomUUID();
        userService.unfollowArtist(artistId);
        verify(userFollowedArtistRepository).deleteByUserIdAndArtistId(userId, artistId);
    }

    @Test
    void bookmarkConcert_Success() {
        UUID concertId = UUID.randomUUID();
        Concert concert = mock(Concert.class);
        when(concert.getId()).thenReturn(concertId);
        when(concertRepository.findById(concertId)).thenReturn(Optional.of(concert));
        when(userLikeConcertRepository.existsByUserIdAndConcertId(userId, concertId)).thenReturn(false);
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        userService.bookmarkConcert(concertId);

        verify(userLikeConcertRepository).save(any(UserLikeConcert.class));
    }

    @Test
    void bookmarkConcert_ConcertNotFound() {
        UUID concertId = UUID.randomUUID();
        when(concertRepository.findById(concertId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.bookmarkConcert(concertId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Concert not found");
    }

    @Test
    void unbookmarkConcert_Success() {
        UUID concertId = UUID.randomUUID();
        userService.unbookmarkConcert(concertId);
        verify(userLikeConcertRepository).deleteByUserIdAndConcertId(userId, concertId);
    }

    @Test
    void getNotificationPreferences_Success() {
        NotificationPreferences prefs = NotificationPreferences.builder()
                .user(user)
                .channelEmailEnabled(true)
                .build();
        when(notificationPreferencesRepository.findByUserId(userId)).thenReturn(Optional.of(prefs));

        NotificationPreferencesDto dto = userService.getNotificationPreferences();

        assertThat(dto).isNotNull();
        assertThat(dto.channelEmailEnabled()).isTrue();
    }

    @Test
    void getNotificationPreferences_NotFound_ReturnsDefault() {
        when(notificationPreferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());

        NotificationPreferencesDto dto = userService.getNotificationPreferences();

        assertThat(dto).isNotNull();
        assertThat(dto.channelEmailEnabled()).isFalse();
    }
}
