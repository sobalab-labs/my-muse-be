package com.sobalab.mymuse.api.security;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import com.sobalab.mymuse.api.model.User;
import com.sobalab.mymuse.api.model.enums.OAuthProvider;
import com.sobalab.mymuse.api.service.AuthService;

class AppOAuth2UserServiceTests {

    @Test
    void malformedKakaoClaimsFallBackToMissingOptionalValues() {
        AuthService authService = mock(AuthService.class);
        DefaultOAuth2UserService delegate = mock(DefaultOAuth2UserService.class);
        AppOAuth2UserService service = new AppOAuth2UserService(authService);
        ReflectionTestUtils.setField(service, "delegate", delegate);

        var registration = org.springframework.security.oauth2.client.registration.ClientRegistration
                .withRegistrationId("kakao")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://example.com/authorize")
                .tokenUri("https://example.com/token")
                .userInfoUri("https://example.com/userinfo")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();
        var now = Instant.now();
        var accessToken = new OAuth2AccessToken(TokenType.BEARER, "token", now, now.plusSeconds(60));
        var request = new OAuth2UserRequest(registration, accessToken);
        Map<String, Object> malformedAccount = Map.of(
                "email", 42,
                "is_email_verified", "yes",
                "profile", "not-an-object");
        var attributes = Map.<String, Object>of("id", 123L, "kakao_account", malformedAccount);
        var oauthUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "id");
        when(delegate.loadUser(request)).thenReturn(oauthUser);
        when(authService.processOAuth2User(
                eq(OAuthProvider.KAKAO), eq("123"), isNull(), eq(false), isNull()))
                .thenReturn(User.builder().name("User").build());

        service.loadUser(request);

        verify(authService).processOAuth2User(
                eq(OAuthProvider.KAKAO), eq("123"), isNull(), eq(false), isNull());
    }
}
