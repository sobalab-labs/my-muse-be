package com.sobalab.mymuse.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_subscription_user", columnNames = { "user_id" })
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreferences extends AuditedEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "channel_email_enabled", nullable = false)
    @Builder.Default
    private boolean channelEmailEnabled = false;

    @Column(name = "channel_kakao_enabled", nullable = false)
    @Builder.Default
    private boolean channelKakaoEnabled = false;

    @Column(name = "general_weekly_updates", nullable = false)
    @Builder.Default
    private boolean generalWeeklyUpdates = false;

    @Column(name = "artist_new_concert_enabled", nullable = false)
    @Builder.Default
    private boolean artistNewConcertEnabled = false;

    @Column(name = "bookmarked_event_ticket_open_enabled", nullable = false)
    @Builder.Default
    private boolean bookmarkedEventTicketOpenEnabled = false;
}
