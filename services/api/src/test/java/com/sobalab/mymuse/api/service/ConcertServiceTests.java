package com.sobalab.mymuse.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sobalab.mymuse.api.model.Concert;
import com.sobalab.mymuse.api.model.Venue;
import com.sobalab.mymuse.api.model.enums.CountryCode;
import com.sobalab.mymuse.api.model.enums.FeedSort;
import com.sobalab.mymuse.api.repository.ConcertRepository;

@ExtendWith(MockitoExtension.class)
class ConcertServiceTests {
    @Mock
    ConcertRepository concertRepository;
    @InjectMocks
    ConcertService concertService;

    @Test
    void detailMapsVenueAndNullableTicketInformation() {
        UUID id = UUID.randomUUID();
        Concert concert = mock(Concert.class);
        Venue venue = mock(Venue.class);
        when(concert.getId()).thenReturn(id);
        when(concert.getTitle()).thenReturn("Live");
        when(concert.getDate()).thenReturn(LocalDateTime.of(2035, 2, 3, 19, 0));
        when(concert.getVenue()).thenReturn(venue);
        when(concert.getTicketingUrl()).thenReturn(null);
        when(concert.getDurationMinutes()).thenReturn(null);
        when(venue.getName()).thenReturn("Arena");
        when(venue.getNation()).thenReturn(CountryCode.JP);
        when(concertRepository.findById(id)).thenReturn(Optional.of(concert));
        var result = concertService.getConcert(id);
        assertThat(result.id()).isEqualTo(id.toString());
        assertThat(result.title()).isEqualTo("Live");
        assertThat(result.place()).isEqualTo("Arena");
        assertThat(result.nation()).isEqualTo("jp");
        assertThat(result.date().toLocalDateTime()).isEqualTo(concert.getDate());
        assertThat(result.ticketingUrl()).isNull();
        assertThat(result.durationMinutes()).isNull();
    }

    @Test
    void missingConcertIsNotFoundAndNullIdNeverReachesRepository() {
        UUID id = UUID.randomUUID();
        when(concertRepository.findById(id)).thenReturn(Optional.empty());
        assertStatus(() -> concertService.getConcert(id), HttpStatus.NOT_FOUND);
        assertStatus(() -> concertService.getConcert(null), HttpStatus.BAD_REQUEST);
        verify(concertRepository).findById(id);
        verifyNoMoreInteractions(concertRepository);
    }

    @Test
    void feedMapsResultsWithoutReorderingThem() {
        Concert first = mock(Concert.class);
        Concert second = mock(Concert.class);
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        when(first.getId()).thenReturn(firstId);
        when(second.getId()).thenReturn(secondId);
        when(concertRepository.findTrendingFeed(any(), any(), any(), any(), any())).thenReturn(List.of(first, second));
        var result = concertService.getTrendingFeed(null, LocalDateTime.of(2035, 1, 1, 0, 0), null,
                FeedSort.DDAY, null, 20);
        assertThat(result).extracting(dto -> dto.id()).containsExactly(firstId.toString(), secondId.toString());
    }

    @Test
    void firstPagePassesTypedFiltersAndUsesStableAnnouncementOrdering() {
        LocalDateTime start = LocalDateTime.of(2400, 2, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1);
        when(concertRepository.findTrendingFeed(any(), any(), any(), any(), any())).thenReturn(List.of());
        assertThat(concertService.getTrendingFeed(CountryCode.JP, start, end,
                FeedSort.RECENTLY_ANNOUNCED, null, 100)).isEmpty();
        var page = ArgumentCaptor.forClass(Limit.class);
        verify(concertRepository).findTrendingFeed(eq(CountryCode.JP), eq(start), eq(end),
                eq(Sort.by("createdAt").descending().and(Sort.by("id").descending())), page.capture());
        assertThat(page.getValue().max()).isEqualTo(100);
        verifyNoMoreInteractions(concertRepository);
    }

    @Test
    void dateCursorUsesDateAndIdRatherThanOffset() {
        UUID id = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.of(2400, 2, 3, 19, 0);
        Concert anchor = mock(Concert.class);
        when(anchor.getDate()).thenReturn(date);
        when(concertRepository.findById(id)).thenReturn(Optional.of(anchor));
        when(concertRepository.findTrendingFeedAfterDate(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        concertService.getTrendingFeed(null, LocalDateTime.of(2035, 1, 1, 0, 0), null, FeedSort.DDAY, id,
                10);
        var page = ArgumentCaptor.forClass(Limit.class);
        verify(concertRepository).findTrendingFeedAfterDate(isNull(), any(), isNull(), eq(date), eq(id),
                page.capture());
        assertThat(page.getValue().max()).isEqualTo(10);
    }

    @Test
    void announcementCursorUsesCreationTimeAndId() {
        UUID id = UUID.randomUUID();
        Instant created = Instant.parse("2035-01-01T00:00:00Z");
        Concert anchor = mock(Concert.class);
        when(anchor.getCreatedAt()).thenReturn(created);
        when(concertRepository.findById(id)).thenReturn(Optional.of(anchor));
        when(concertRepository.findTrendingFeedAfterAnnouncement(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        concertService.getTrendingFeed(null, LocalDateTime.of(2035, 1, 1, 0, 0), null,
                FeedSort.RECENTLY_ANNOUNCED, id, 10);
        verify(concertRepository).findTrendingFeedAfterAnnouncement(isNull(), any(), isNull(), eq(created), eq(id),
                any());
    }

    @Test
    void deletedCursorIsBadRequestRatherThanRepeatingFirstPage() {
        UUID id = UUID.randomUUID();
        when(concertRepository.findById(id)).thenReturn(Optional.empty());
        assertStatus(
                () -> concertService.getTrendingFeed(null, LocalDateTime.of(2035, 1, 1, 0, 0), null,
                        FeedSort.DDAY, id, 20),
                HttpStatus.BAD_REQUEST);
        verify(concertRepository).findById(id);
        verifyNoMoreInteractions(concertRepository);
    }

    private static void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(status));
    }
}
