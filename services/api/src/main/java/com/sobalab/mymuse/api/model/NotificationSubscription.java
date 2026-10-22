package com.sobalab.mymuse.api.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_subscriptions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_notification_subscription_user", columnNames = {"user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSubscription extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = false;

    @Column(name = "kakao_enabled", nullable = false)
    private boolean kakaoEnabled = false;

    @Column(name = "general_weekly_updates", nullable = false)
    private boolean generalWeeklyUpdates = false;
}
