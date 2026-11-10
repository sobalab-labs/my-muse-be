package com.sobalab.mymuse.api.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sobalab.mymuse.api.dto.CursorResponse;
import com.sobalab.mymuse.api.dto.SearchResponse;
import com.sobalab.mymuse.api.model.enums.SearchType;
import com.sobalab.mymuse.api.security.AppOAuth2UserService;
import com.sobalab.mymuse.api.security.AppOidcUserService;
import com.sobalab.mymuse.api.service.SearchService;

@WebMvcTest(SearchController.class)
@WithMockUser
class SearchControllerTests {
    @Autowired private MockMvc mockMvc;

    @MockitoBean private SearchService searchService;
    @MockitoBean private AppOidcUserService appOidcUserService;
    @MockitoBean private AppOAuth2UserService appOAuth2UserService;

    @Test
    void defaultsToBothTypesAndLimitAndReturnsBothPages() throws Exception {
        SearchResponse response = new SearchResponse(
                new CursorResponse<>(List.of(), "artist-next", true),
                new CursorResponse<>(List.of(), "concert-next", false));
        given(searchService.search("BLACKPINK", List.of(SearchType.ARTIST, SearchType.CONCERT), 20, null, null))
                .willReturn(response);

        mockMvc.perform(get("/search").param("q", "BLACKPINK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artists.nextCursor").value("artist-next"))
                .andExpect(jsonPath("$.artists.hasNext").value(true))
                .andExpect(jsonPath("$.concerts.nextCursor").value("concert-next"))
                .andExpect(jsonPath("$.concerts.hasNext").value(false));

        verify(searchService).search("BLACKPINK", List.of(SearchType.ARTIST, SearchType.CONCERT), 20, null, null);
    }

    @Test
    void bindsTypeLimitAndBothIndependentCursors() throws Exception {
        given(searchService.search("BLACKPINK", List.of(SearchType.ARTIST), 5, "a-cursor", "c-cursor"))
                .willReturn(new SearchResponse(
                        new CursorResponse<>(List.of(), null, false),
                        new CursorResponse<>(List.of(), null, false)));

        mockMvc.perform(get("/search").param("q", "BLACKPINK")
                .param("types", "artist").param("limit", "5")
                .param("artistCursor", "a-cursor").param("concertCursor", "c-cursor"))
                .andExpect(status().isOk());

        verify(searchService).search("BLACKPINK", List.of(SearchType.ARTIST), 5, "a-cursor", "c-cursor");
    }

    @ParameterizedTest
    @ValueSource(strings = { "venue", "artist,venue", "artist,", ",concert", "," })
    void rejectsInvalidTypesBeforeCallingService(String types) throws Exception {
        mockMvc.perform(get("/search").param("q", "BLACKPINK").param("types", types))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(searchService);
    }

    @Test
    void missingQueryIsBadRequest() throws Exception {
        mockMvc.perform(get("/search")).andExpect(status().isBadRequest());
        verifyNoInteractions(searchService);
    }

    @Test
    void blankQueryIsBadRequest() throws Exception {
        mockMvc.perform(get("/search").param("q", "   ")).andExpect(status().isBadRequest());
        verifyNoInteractions(searchService);
    }
}
