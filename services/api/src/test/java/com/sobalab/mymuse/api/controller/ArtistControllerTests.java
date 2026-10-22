package com.sobalab.mymuse.api.controller;

import com.sobalab.mymuse.api.service.ArtistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest (ArtistController.class)
public class ArtistControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    @Test
    void listArtists_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/artists"))
                .andExpect(status().isOk());
    }

    @Test
    void getArtist_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/artists/123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(status().isOk());
    }
}
