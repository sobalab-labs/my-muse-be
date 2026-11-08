package com.sobalab.mymuse.api.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sobalab.mymuse.api.model.enums.OAuthProvider;
import com.sobalab.mymuse.api.security.AppOAuth2UserService;
import com.sobalab.mymuse.api.security.AppOidcUserService;
import com.sobalab.mymuse.api.service.AuthService;

@WebMvcTest(AuthController.class)
@WithMockUser
public class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AppOidcUserService appOidcUserService;

    @MockitoBean
    private AppOAuth2UserService appOAuth2UserService;

    @Test
    void login_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\": \"google\", \"code\": \"auth-code-123\"}"))
                .andExpect(status().isOk());
        verify(authService).loginOAuth(OAuthProvider.GOOGLE, "auth-code-123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"unknown\"", "null", "\"\"", "0"})
    void login_InvalidProvider_ShouldRejectBeforeService(String provider) throws Exception {
        mockMvc.perform(post("/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":" + provider + ",\"code\":\"valid-code\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test
    void login_Kakao_ShouldBindProvider() throws Exception {
        mockMvc.perform(post("/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"kakao\",\"code\":\"valid-code\"}"))
                .andExpect(status().isOk());
        verify(authService).loginOAuth(OAuthProvider.KAKAO, "valid-code");
    }

    @Test
    void login_MissingFields_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\": \"\", \"code\": \"\"}"))
                .andExpect(status().isBadRequest());
    }
}
