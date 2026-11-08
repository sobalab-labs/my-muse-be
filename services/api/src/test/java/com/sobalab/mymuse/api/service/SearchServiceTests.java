package com.sobalab.mymuse.api.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sobalab.mymuse.api.model.enums.SearchType;

class SearchServiceTests {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void invalidKeywordIsRejectedBeforeCallingDownstreamServices(String query) {
        ArtistService artists = mock(ArtistService.class);
        ConcertService concerts = mock(ConcertService.class);
        SearchService service = new SearchService(artists, concerts);

        assertThatThrownBy(() -> service.search(query, List.of(SearchType.ARTIST, SearchType.CONCERT), 20))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(artists, concerts);
    }
}
