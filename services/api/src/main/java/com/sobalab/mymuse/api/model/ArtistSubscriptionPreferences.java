package com.sobalab.mymuse.api.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "artist_subscription_preferences", uniqueConstraints = {
    @UniqueConstraint(name = "uk_artist_subscription_preference_user", columnNames = {"user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistSubscriptionPreferences extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "new_concerts_enabled", nullable = false)
    private boolean newConcertsEnabled = false;

    @Column(name = "ticket_open_bundle_enabled", nullable = false)
    private boolean ticketOpenBundleEnabled = false;

    @Column(name = "discography_enabled", nullable = false)
    private boolean discographyEnabled = false;
}
