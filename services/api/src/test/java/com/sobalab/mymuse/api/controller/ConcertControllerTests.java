package com.sobalab.mymuse.api.controller;

import com.sobalab.mymuse.api.service.ConcertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConcertController.class)
public class ConcertControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean 
    private ConcertService concertService;

    @Test
    void listConcerts_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/concerts")
                        .param("scope", "following")
                        .param("artistId", "123e4567-e89b-12d3-a456-426614174000")
                        .param("region", "korea"))
                .andExpect(status().isOk());
    }

    @Test
    void getConcert_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/concerts/123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(status().isOk());
    }
}
