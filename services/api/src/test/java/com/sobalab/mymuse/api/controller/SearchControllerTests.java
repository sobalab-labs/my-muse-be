package com.sobalab.mymuse.api.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sobalab.mymuse.api.dto.ArtistDto;
import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.dto.CursorResponse;
import com.sobalab.mymuse.api.dto.SearchResponse;
import com.sobalab.mymuse.api.model.enums.SearchType;
import com.sobalab.mymuse.api.security.AppOAuth2UserService;
import com.sobalab.mymuse.api.security.AppOidcUserService;
import com.sobalab.mymuse.api.service.SearchService;

@WebMvcTest(SearchController.class)
@WithMockUser
public class SearchControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @ParameterizedTest
    @ValueSource(strings = {"venue", "artist,venue", "artist,", ",concert", ","})
    void search_InvalidTypes_ShouldRejectBeforeService(String types) throws Exception {
        mockMvc.perform(get("/api/search").param("q", "BLACKPINK").param("types", types))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(searchService);
    }

    @Test
    void search_RepeatedTypes_ShouldBindEnumList() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "BLACKPINK")
                .param("types", "artist", "concert"))
                .andExpect(status().isOk());
        verify(searchService).search("BLACKPINK", List.of(SearchType.ARTIST, SearchType.CONCERT), 20);
    }

    @MockitoBean
    private AppOidcUserService appOidcUserService;

    @MockitoBean
    private AppOAuth2UserService appOAuth2UserService;

    @Nested
    @DisplayName("GET /api/search")
    class AggregateSearchTests {

        @Test
        @DisplayName("Should return search results with default parameters")
        void search_WithValidQuery_ShouldReturnOkAndDelegateToService() throws Exception {
            SearchResponse mockResponse = new SearchResponse(
                    new CursorResponse<>(List.of(), "artist-cursor", true),
                    new CursorResponse<>(List.of(), "concert-cursor", false)
            );
            given(searchService.search("BLACKPINK", List.of(SearchType.ARTIST, SearchType.CONCERT), 20)).willReturn(mockResponse);

            mockMvc.perform(get("/api/search").param("q", "BLACKPINK"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.artists.nextCursor").value("artist-cursor"))
                    .andExpect(jsonPath("$.artists.hasNext").value(true))
                    .andExpect(jsonPath("$.concerts.nextCursor").value("concert-cursor"))
                    .andExpect(jsonPath("$.concerts.hasNext").value(false));

            verify(searchService).search("BLACKPINK", List.of(SearchType.ARTIST, SearchType.CONCERT), 20);
        }

        @Test
        @DisplayName("Should handle custom types and limit parameters")
        void search_WithCustomTypesAndLimit_ShouldReturnOk() throws Exception {
            SearchResponse mockResponse = new SearchResponse(
                    new CursorResponse<>(List.of(), null, false),
                    new CursorResponse<>(List.of(), null, false)
            );
            given(searchService.search("BLACKPINK", List.of(SearchType.ARTIST), 5)).willReturn(mockResponse);

            mockMvc.perform(get("/api/search")
                            .param("q", "BLACKPINK")
                            .param("types", "artist")
                            .param("limit", "5"))
                    .andExpect(status().isOk());

            verify(searchService).search("BLACKPINK", List.of(SearchType.ARTIST), 5);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when 'q' parameter is missing")
        void search_WithMissingQuery_ShouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/api/search"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when 'q' parameter is blank")
        void search_WithBlankQuery_ShouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/api/search").param("q", "   "))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/search/artists")
    class ArtistSearchTests {

        @Test
        @DisplayName("Should return artist page with default limit")
        void searchArtists_WithValidCursor_ShouldReturnOk() throws Exception {
            CursorResponse<ArtistDto> mockResponse = new CursorResponse<>(List.of(), "next-artist-cursor", true);
            given(searchService.searchArtists("cursor123", 10)).willReturn(mockResponse);

            mockMvc.perform(get("/api/search/artists").param("cursor", "cursor123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nextCursor").value("next-artist-cursor"))
                    .andExpect(jsonPath("$.hasNext").value(true));

            verify(searchService).searchArtists("cursor123", 10);
        }

        @Test
        @DisplayName("Should respect custom limit parameter")
        void searchArtists_WithCustomLimit_ShouldReturnOk() throws Exception {
            CursorResponse<ArtistDto> mockResponse = new CursorResponse<>(List.of(), null, false);
            given(searchService.searchArtists("cursor123", 5)).willReturn(mockResponse);

            mockMvc.perform(get("/api/search/artists")
                            .param("cursor", "cursor123")
                            .param("limit", "5"))
                    .andExpect(status().isOk());

            verify(searchService).searchArtists("cursor123", 5);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when 'cursor' parameter is missing")
        void searchArtists_WithMissingCursor_ShouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/api/search/artists"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/search/concerts")
    class ConcertSearchTests {

        @Test
        @DisplayName("Should return concert page with default limit")
        void searchConcerts_WithValidCursor_ShouldReturnOk() throws Exception {
            CursorResponse<ConcertDto> mockResponse = new CursorResponse<>(List.of(), "next-concert-cursor", false);
            given(searchService.searchConcerts("cursor456", 20)).willReturn(mockResponse);

            mockMvc.perform(get("/api/search/concerts").param("cursor", "cursor456"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nextCursor").value("next-concert-cursor"))
                    .andExpect(jsonPath("$.hasNext").value(false));

            verify(searchService).searchConcerts("cursor456", 20);
        }

        @Test
        @DisplayName("Should respect custom limit parameter")
        void searchConcerts_WithCustomLimit_ShouldReturnOk() throws Exception {
            CursorResponse<ConcertDto> mockResponse = new CursorResponse<>(List.of(), null, false);
            given(searchService.searchConcerts("cursor456", 15)).willReturn(mockResponse);

            mockMvc.perform(get("/api/search/concerts")
                            .param("cursor", "cursor456")
                            .param("limit", "15"))
                    .andExpect(status().isOk());

            verify(searchService).searchConcerts("cursor456", 15);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when 'cursor' parameter is missing")
        void searchConcerts_WithMissingCursor_ShouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/api/search/concerts"))
                    .andExpect(status().isBadRequest());
        }
    }
}
