package com.sobalab.mymuse.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sobalab.mymuse.api.security.AppOAuth2UserService;
import com.sobalab.mymuse.api.security.AppOidcUserService;
import com.sobalab.mymuse.api.service.ConcertService;

@WebMvcTest(FeedController.class)
@WithMockUser
public class FeedControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConcertService concertService;

    @MockitoBean
    private AppOidcUserService appOidcUserService;

    @MockitoBean
    private AppOAuth2UserService appOAuth2UserService;

    @Test
    void getTrendingFeed_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/feed/trending")
                .param("view", "list")
                .param("month", "2026-11")
                .param("region", "korea")
                .param("sort", "dday")
                .param("cursor", "abc")
                .param("limit", "20"))
                .andExpect(status().isOk());
    }
}
