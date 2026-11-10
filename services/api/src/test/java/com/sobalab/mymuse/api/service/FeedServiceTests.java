package com.sobalab.mymuse.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.model.enums.CountryCode;
import com.sobalab.mymuse.api.model.enums.FeedSort;
import com.sobalab.mymuse.api.model.enums.FeedView;

@ExtendWith(MockitoExtension.class)
class FeedServiceTests {
    @Mock
    ConcertService concertService;
    @InjectMocks
    FeedService feedService;

    @Test
    void defaultRequestDelegatesTypedFutureRangeAndDefaultPageSize() {
        when(concertService.getTrendingFeed(any(), any(), any(), any(), any(), anyInt())).thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now();
        assertThat(feedService.getTrendingFeed(null, null, null, null, null, null).items()).isEmpty();
        LocalDateTime after = LocalDateTime.now();
        var start = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(concertService).getTrendingFeed(isNull(), start.capture(), isNull(), eq(FeedSort.DDAY), isNull(),
                eq(21));
        assertThat(start.getValue()).isBetween(before, after);
    }

    @Test
    void calendarUsesExclusiveEndOfLeapMonth() {
        UUID cursor = UUID.randomUUID();
        when(concertService.getTrendingFeed(any(), any(), any(), any(), any(), anyInt())).thenReturn(List.of());
        feedService.getTrendingFeed(FeedView.CALENDAR, "2400-02", CountryCode.JP, FeedSort.RECENTLY_ANNOUNCED, cursor.toString(), 100);
        verify(concertService).getTrendingFeed(eq(CountryCode.JP), eq(LocalDateTime.of(2400, 2, 1, 0, 0)),
                eq(LocalDateTime.of(2400, 3, 1, 0, 0)), eq(FeedSort.RECENTLY_ANNOUNCED), eq(cursor), eq(101));
    }

    @Test
    void currentMonthStartsNow() {
        LocalDateTime before = LocalDateTime.now();
        var month = java.time.YearMonth.from(before);
        when(concertService.getTrendingFeed(any(), any(), any(), any(), any(), anyInt())).thenReturn(List.of());
        feedService.getTrendingFeed(FeedView.CALENDAR, month.toString(), null, null, null, 20);
        LocalDateTime after = LocalDateTime.now();
        var start = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(concertService).getTrendingFeed(isNull(), start.capture(),
                eq(month.plusMonths(1).atDay(1).atStartOfDay()), eq(FeedSort.DDAY), isNull(), eq(21));
        assertThat(start.getValue()).isBetween(before, after);
    }

    @Test
    void pastCalendarReturnsEmptyWithoutCallingConcertService() {
        assertThat(feedService.getTrendingFeed(FeedView.CALENDAR, "2000-01", null, null, null, 20).items()).isEmpty();
        verifyNoInteractions(concertService);
    }

    @ParameterizedTest
    @CsvSource({ "CALENDAR,,,,,20", "LIST,invalid,,,,20", "CALENDAR,2025-13,,,,20",
            "CALENDAR,+999999999-12,,,,20",
            "LIST,,,,broken,20", "LIST,,,,1-1-1-1-1,20", "LIST,,,,,0", "LIST,,,,,-1", "LIST,,,,,101" })
    void invalidRequestNeverReachesConcertService(FeedView view, String month, CountryCode nation,
            FeedSort sort, String cursor, Integer limit) {
        assertThatThrownBy(() -> feedService.getTrendingFeed(view, month, nation, sort, cursor, limit))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(concertService);
    }

    @Test
    void resultOrderAndContentArePreserved() {
        List<ConcertDto> concerts = List.of(
                new ConcertDto("first", "Live", null, null, null, null, null),
                new ConcertDto("second", "Live", null, null, null, null, null),
                new ConcertDto("third", "Live", null, null, null, null, null));
        when(concertService.getTrendingFeed(any(), any(), any(), any(), any(), anyInt())).thenReturn(concerts);
        var response = feedService.getTrendingFeed(null, null, null, null, null, 2);
        assertThat(response.items()).extracting(ConcertDto::id)
                .containsExactly("first", "second");
        assertThat(response.nextCursor()).isEqualTo("second");
        assertThat(response.hasNext()).isTrue();
        verify(concertService).getTrendingFeed(isNull(), any(), isNull(), eq(FeedSort.DDAY), isNull(), eq(3));
    }

    @Test
    void concertServiceFailureIsNotHidden() {
        var failure = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor concert no longer exists");
        when(concertService.getTrendingFeed(any(), any(), any(), any(), any(), anyInt())).thenThrow(failure);
        assertThatThrownBy(() -> feedService.getTrendingFeed(null, null, null, null, null, 20)).isSameAs(failure);
    }
}
