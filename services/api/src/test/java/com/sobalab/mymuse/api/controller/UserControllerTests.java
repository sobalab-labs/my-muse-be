package com.sobalab.mymuse.api.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sobalab.mymuse.api.model.enums.FeedSort;
import com.sobalab.mymuse.api.model.enums.FeedView;
import com.sobalab.mymuse.api.security.AppOAuth2UserService;
import com.sobalab.mymuse.api.security.AppOidcUserService;
import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.dto.CursorResponse;
import com.sobalab.mymuse.api.service.UserService;

@WebMvcTest(UserController.class)
@WithMockUser
public class UserControllerTests {

    private static final String VALID_ARTIST_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String VALID_CONCERT_ID = "123e4567-e89b-12d3-a456-426614174001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @ParameterizedTest
    @CsvSource({"list,dday", "calendar,recently_announced"})
    void getFeed_ShouldBindEnumOptions(String view, String sort) throws Exception {
        mockMvc.perform(get("/users/me/feed").param("view", view).param("sort", sort))
                .andExpect(status().isOk());
        verify(userService).getFeed(FeedView.valueOf(view.toUpperCase(java.util.Locale.ROOT)), null, FeedSort.valueOf(sort.toUpperCase(java.util.Locale.ROOT)), null, null);
    }

    @Test
    void getFeed_ShouldReturnServiceCursorMetadata() throws Exception {
        var item = new ConcertDto("concert-2", "Live", null, null, null, null, null);
        when(userService.getFeed(null, null, null, null, 2))
                .thenReturn(new CursorResponse<>(java.util.List.of(item), "concert-2", true));

        mockMvc.perform(get("/users/me/feed").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("concert-2"))
                .andExpect(jsonPath("$.nextCursor").value("concert-2"))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @ParameterizedTest
    @CsvSource({"view,grid", "sort,popular"})
    void getFeed_InvalidOption_ShouldRejectBeforeService(String parameter, String value) throws Exception {
        mockMvc.perform(get("/users/me/feed").param(parameter, value))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }

    @MockitoBean
    private AppOidcUserService appOidcUserService;

    @MockitoBean
    private AppOAuth2UserService appOAuth2UserService;

    @Test
    void getMe_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteMe_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/users/me").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getFollowedArtists_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/users/me/followed-artists"))
                .andExpect(status().isOk());
    }

    @Test
    void followArtist_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/users/me/followed-artists")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"artistId\": \"" + VALID_ARTIST_ID + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void followArtist_MissingArtistId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/users/me/followed-artists")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"artistId\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unfollowArtist_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/users/me/followed-artists/" + VALID_ARTIST_ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getBookmarkedConcerts_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/users/me/bookmarked-concerts"))
                .andExpect(status().isOk());
    }

    @Test
    void bookmarkConcert_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/users/me/bookmarked-concerts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concertId\": \"" + VALID_CONCERT_ID + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void bookmarkConcert_MissingConcertId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/users/me/bookmarked-concerts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concertId\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unbookmarkConcert_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/users/me/bookmarked-concerts/" + VALID_CONCERT_ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getNotificationPreferences_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/users/me/notification-preferences"))
                .andExpect(status().isOk());
    }

    @Test
    void patchNotificationPreferences_ShouldReturnOk() throws Exception {
        mockMvc.perform(patch("/users/me/notification-preferences")
                        .with(csrf())
                        .contentType("application/merge-patch+json")
                        .content("{\"topics\": {\"bookmarkedEventTicketOpen\": false}}"))
                .andExpect(status().isOk());
    }

    @Test
    void patchNotificationPreferences_EmptyBody_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(patch("/users/me/notification-preferences")
                        .with(csrf())
                        .contentType("application/merge-patch+json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeOnboarding_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/users/me/onboarding")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followedArtistIds\": [\"" + VALID_ARTIST_ID + "\"]}"))
                .andExpect(status().isOk());
    }
}
