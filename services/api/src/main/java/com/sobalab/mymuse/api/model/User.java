package com.sobalab.mymuse.api.model;

import com.sobalab.mymuse.api.model.enums.OAuthProvider;
import com.sobalab.mymuse.api.model.enums.PreferredLocale;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_oauth", columnNames = {"oauth_provider", "oauth_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false)
    private OAuthProvider oauthProvider;

    @Column(name = "oauth_id", nullable = false)
    private String oauthId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_locale", nullable = false)
    private PreferredLocale preferredLocale;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;
}
