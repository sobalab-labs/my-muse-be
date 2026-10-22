package com.sobalab.mymuse.api.controller;

import com.sobalab.mymuse.api.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getMe_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteMe_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/users/me"))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"artistId\": \"123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void unfollowArtist_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/users/me/followed-artists/123"))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concertId\": \"123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void unbookmarkConcert_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/users/me/bookmarked-concerts/123"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getNotificationSubscription_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/users/me/notification-subscription"))
                .andExpect(status().isOk());
    }

    @Test
    void updateNotificationSubscription_ShouldReturnOk() throws Exception {
        mockMvc.perform(put("/users/me/notification-subscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void completeOnboarding_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/users/me/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followedArtistIds\": []}"))
                .andExpect(status().isOk());
    }
}
