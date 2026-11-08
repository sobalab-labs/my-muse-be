package com.sobalab.mymuse.api.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.sobalab.mymuse.api.model.enums.SupportedLocale;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditedEntity {

    @Column
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "preferred_locale", columnDefinition = "locale", nullable = false)
    private SupportedLocale preferredLocale = SupportedLocale.KO;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserIdentity> userIdentities = new ArrayList<>();

    @Builder
    public User(String email, boolean emailVerified, String name) {
        this.email = email;
        this.emailVerified = emailVerified;
        this.name = name != null ? name : "User";
        this.onboardingCompleted = false;

    }
}
