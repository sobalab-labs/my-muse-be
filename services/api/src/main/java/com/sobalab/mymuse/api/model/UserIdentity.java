package com.sobalab.mymuse.api.model;

import com.sobalab.mymuse.api.model.enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_identities", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_identity_provider_subject", columnNames = {"provider", "provider_subject"})
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserIdentity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private OAuthProvider provider;

    @Column(name = "provider_subject", nullable = false)
    private String providerSubject;
}
