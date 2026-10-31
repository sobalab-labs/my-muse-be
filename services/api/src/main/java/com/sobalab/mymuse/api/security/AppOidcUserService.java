package com.sobalab.mymuse.api.security;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.sobalab.mymuse.api.model.User;
import com.sobalab.mymuse.api.model.enums.OAuthProvider;
import com.sobalab.mymuse.api.service.AuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppOidcUserService extends OidcUserService {

    private final AuthService authService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String subject = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        Boolean emailVerified = oidcUser.getEmailVerified();
        String name = oidcUser.getFullName();
        if (name == null) {
            name = oidcUser.getNickName();
        }
        if (name == null) {
            name = oidcUser.getAttribute("name");
        }

        User user = authService.processOAuth2User(
                OAuthProvider.GOOGLE,
                subject,
                email,
                emailVerified != null && emailVerified,
                name);

        return new AppUserPrincipal(user, oidcUser.getAttributes(), oidcUser.getIdToken(), oidcUser.getUserInfo());
    }
}
