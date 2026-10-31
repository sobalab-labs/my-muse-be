package com.sobalab.mymuse.api.security;

import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.sobalab.mymuse.api.model.User;
import com.sobalab.mymuse.api.model.enums.OAuthProvider;
import com.sobalab.mymuse.api.service.AuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final AuthService authService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        if ("kakao".equals(registrationId)) {
            Object idObj = oauth2User.getAttribute("id");
            String subject = idObj != null ? String.valueOf(idObj) : null;

            Map<String, Object> kakaoAccount = oauth2User.getAttribute("kakao_account");
            String email = null;
            boolean emailVerified = false;
            String nickname = null;

            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
                Boolean isEmailVerified = (Boolean) kakaoAccount.get("is_email_verified");
                if (isEmailVerified != null) {
                    emailVerified = isEmailVerified;
                }
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    nickname = (String) profile.get("nickname");
                }
            }

            User user = authService.processOAuth2User(
                    OAuthProvider.KAKAO,
                    subject,
                    email,
                    emailVerified,
                    nickname);

            return new AppUserPrincipal(user, oauth2User.getAttributes());
        }

        throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
    }
}
