package com.sobalab.mymuse.api.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sobalab.mymuse.api.model.enums.CountryCode;
import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.dto.CursorResponse;
import com.sobalab.mymuse.api.model.enums.FeedSort;
import com.sobalab.mymuse.api.model.enums.FeedView;
import com.sobalab.mymuse.api.security.AppOAuth2UserService;
import com.sobalab.mymuse.api.security.AppOidcUserService;
import com.sobalab.mymuse.api.service.FeedService;

@WebMvcTest(FeedController.class)
@WithMockUser
public class FeedControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedService feedService;

    @MockitoBean
    private AppOidcUserService appOidcUserService;

    @MockitoBean
    private AppOAuth2UserService appOAuth2UserService;

    @Test
    void getTrendingFeed_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/feed/trending")
                .param("view", "list")
                .param("month", "2026-11")
                .param("nation", "KR")
                .param("sort", "dday")
                .param("cursor", "abc")
                .param("limit", "20"))
                .andExpect(status().isOk());
        verify(feedService).getTrendingFeed(FeedView.LIST, "2026-11", CountryCode.KR, FeedSort.DDAY, "abc", 20);
    }

    @Test
    void getTrendingFeed_ShouldReturnServiceCursorMetadata() throws Exception {
        var item = new ConcertDto("concert-2", "Live", null, null, null, null, null);
        when(feedService.getTrendingFeed(null, null, null, null, null, 2))
                .thenReturn(new CursorResponse<>(java.util.List.of(item), "concert-2", true));

        mockMvc.perform(get("/feed/trending").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("concert-2"))
                .andExpect(jsonPath("$.nextCursor").value("concert-2"))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void getTrendingFeed_ShouldBindCalendarAndRecentlyAnnounced() throws Exception {
        mockMvc.perform(get("/feed/trending")
                .param("view", " Calendar ")
                .param("month", "2026-11")
                .param("sort", " RECENTLY_ANNOUNCED "))
                .andExpect(status().isOk());
        verify(feedService).getTrendingFeed(FeedView.CALENDAR, "2026-11", null, FeedSort.RECENTLY_ANNOUNCED, null, null);
    }

    @Test
    void getTrendingFeed_ShouldAllowOmittedOptions() throws Exception {
        mockMvc.perform(get("/feed/trending")).andExpect(status().isOk());
        verify(feedService).getTrendingFeed(null, null, null, null, null, null);
    }

    @ParameterizedTest
    @CsvSource({"view,grid", "sort,popular"})
    void getTrendingFeed_ShouldRejectUnknownOptions(String parameter, String value) throws Exception {
        mockMvc.perform(get("/feed/trending").param(parameter, value))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(feedService);
    }
}
