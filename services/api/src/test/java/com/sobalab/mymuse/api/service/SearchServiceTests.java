package com.sobalab.mymuse.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sobalab.mymuse.api.dto.ArtistDto;
import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.dto.CursorResponse;
import com.sobalab.mymuse.api.dto.SearchResponse;
import com.sobalab.mymuse.api.model.enums.SearchType;

class SearchServiceTests {
    private ArtistService artistService;
    private ConcertService concertService;
    private SearchService searchService;

    private final CursorResponse<ArtistDto> artistPage = new CursorResponse<>(List.of(), "artist-next", true);
    private final CursorResponse<ConcertDto> concertPage = new CursorResponse<>(List.of(), "concert-next", true);

    @BeforeEach
    void setUp() {
        artistService = mock(ArtistService.class);
        concertService = mock(ConcertService.class);
        searchService = new SearchService(artistService, concertService);
    }

    @Test
    void defaultsToBothTypesAndDefaultPageSize() {
        when(artistService.search("live", null, 20)).thenReturn(artistPage);
        when(concertService.search("live", null, 20)).thenReturn(concertPage);

        SearchResponse result = searchService.search("live", null, null);

        assertThat(result.artists()).isSameAs(artistPage);
        assertThat(result.concerts()).isSameAs(concertPage);
        verify(artistService).search("live", null, 20);
        verify(concertService).search("live", null, 20);
    }

    @Test
    void emptyTypesAlsoDefaultsToBoth() {
        when(artistService.search("live", null, 20)).thenReturn(artistPage);
        when(concertService.search("live", null, 20)).thenReturn(concertPage);

        SearchResponse result = searchService.search("live", List.of(), null);

        assertThat(result).isEqualTo(new SearchResponse(artistPage, concertPage));
    }

    @Test
    void searchesBothTypesWithNormalizedQueryAndIndependentCursors() {
        when(artistService.search("live music", "a-cursor", 7)).thenReturn(artistPage);
        when(concertService.search("live music", "c-cursor", 7)).thenReturn(concertPage);

        SearchResponse result = searchService.search("  live music \t", List.of(SearchType.ARTIST, SearchType.CONCERT),
                7, "a-cursor", "c-cursor");

        assertThat(result).isEqualTo(new SearchResponse(artistPage, concertPage));
        verify(artistService).search("live music", "a-cursor", 7);
        verify(concertService).search("live music", "c-cursor", 7);
    }

    @Test
    void artistOnlySearchReturnsEmptyConcertPageAndDoesNotCallConcertService() {
        when(artistService.search("live", "artist-cursor", 5)).thenReturn(artistPage);

        SearchResponse result = searchService.search("live", List.of(SearchType.ARTIST), 5, "artist-cursor", "unused");

        assertThat(result.artists()).isSameAs(artistPage);
        assertThat(result.concerts()).isEqualTo(new CursorResponse<>(List.of(), null, false));
        verify(artistService).search("live", "artist-cursor", 5);
        verifyNoInteractions(concertService);
    }

    @Test
    void concertOnlySearchReturnsEmptyArtistPageAndDoesNotCallArtistService() {
        when(concertService.search("live", "concert-cursor", 5)).thenReturn(concertPage);

        SearchResponse result = searchService.search("live", List.of(SearchType.CONCERT), 5, "unused", "concert-cursor");

        assertThat(result.artists()).isEqualTo(new CursorResponse<>(List.of(), null, false));
        assertThat(result.concerts()).isSameAs(concertPage);
        verify(concertService).search("live", "concert-cursor", 5);
        verifyNoInteractions(artistService);
    }

    @Test
    void threeArgumentOverloadPassesNoCursors() {
        when(artistService.search("live", null, 3)).thenReturn(artistPage);
        when(concertService.search("live", null, 3)).thenReturn(concertPage);

        searchService.search("live", List.of(SearchType.ARTIST, SearchType.CONCERT), 3);

        verify(artistService).search("live", null, 3);
        verify(concertService).search("live", null, 3);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t\n" })
    void rejectsNullOrBlankQueryBeforeCallingDownstreamServices(String query) {
        assertBadRequest(() -> searchService.search(query, List.of(SearchType.ARTIST, SearchType.CONCERT), 20));
        verifyNoInteractions(artistService, concertService);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -20, 101, 1000 })
    void rejectsOutOfRangeLimitsBeforeCallingDownstreamServices(int limit) {
        assertBadRequest(() -> searchService.search("live", List.of(SearchType.ARTIST, SearchType.CONCERT), limit));
        verifyNoInteractions(artistService, concertService);
    }

    @Test
    void acceptsMinimumLimit() {
        when(artistService.search("live", null, 1)).thenReturn(artistPage);
        when(concertService.search("live", null, 1)).thenReturn(concertPage);

        searchService.search("live", List.of(SearchType.ARTIST, SearchType.CONCERT), 1);

        verify(artistService).search("live", null, 1);
        verify(concertService).search("live", null, 1);
    }

    @Test
    void acceptsMaximumLimit() {
        when(artistService.search("live", null, 100)).thenReturn(artistPage);
        when(concertService.search("live", null, 100)).thenReturn(concertPage);

        searchService.search("live", List.of(SearchType.ARTIST, SearchType.CONCERT), 100);

        verify(artistService).search("live", null, 100);
        verify(concertService).search("live", null, 100);
    }

    private static void assertBadRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
