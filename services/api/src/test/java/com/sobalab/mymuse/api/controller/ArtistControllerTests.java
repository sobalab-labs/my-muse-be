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
import com.sobalab.mymuse.api.service.ArtistService;

@WebMvcTest(ArtistController.class)
@WithMockUser
public class ArtistControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private AppOidcUserService appOidcUserService;

    @MockitoBean
    private AppOAuth2UserService appOAuth2UserService;

    @Test
    void getArtist_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/artists/123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(status().isOk());
    }
}
