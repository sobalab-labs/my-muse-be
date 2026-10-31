package com.sobalab.mymuse.api.security;

import com.sobalab.mymuse.api.model.User;
import com.sobalab.mymuse.api.model.enums.PreferredLocale;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class AppUserPrincipal implements OidcUser, OAuth2User {

    private final UUID userId;
    private final String email;
    private final boolean emailVerified;
    private final String name;
    private final PreferredLocale preferredLocale;
    private final boolean onboardingCompleted;
    private final Map<String, Object> attributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    public AppUserPrincipal(User user, Map<String, Object> attributes, OidcIdToken idToken, OidcUserInfo userInfo) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.emailVerified = user.isEmailVerified();
        this.name = user.getName();
        this.preferredLocale = user.getPreferredLocale();
        this.onboardingCompleted = user.isOnboardingCompleted();
        this.attributes = attributes;
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    public AppUserPrincipal(User user, Map<String, Object> attributes) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.emailVerified = user.isEmailVerified();
        this.name = user.getName();
        this.preferredLocale = user.getPreferredLocale();
        this.onboardingCompleted = user.isOnboardingCompleted();
        this.attributes = attributes;
        this.idToken = null;
        this.userInfo = null;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return userId.toString();
    }

    @Override
    public Map<String, Object> getClaims() {
        return idToken != null ? idToken.getClaims() : attributes;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }
}
