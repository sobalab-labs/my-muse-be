package com.sobalab.mymuse.api.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "concerts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Concert extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "ticketing_url")
    private String ticketingUrl;

    @Column(name = "official_announcement_url", nullable = false)
    private String officialAnnouncementUrl;
}
