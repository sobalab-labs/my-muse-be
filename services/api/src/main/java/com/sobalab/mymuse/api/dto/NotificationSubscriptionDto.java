package com.sobalab.mymuse.api.dto;

import java.util.List;

public record NotificationSubscriptionDto(
        ChannelConfig channels,
        List<ArtistSubscriptionConfig> artistSubscriptions,
        boolean generalWeeklyUpdates
) {
    public record ChannelConfig(boolean email, boolean kakao) {}
    public record ArtistSubscriptionConfig(
            String artistId,
            boolean newConcerts,
            boolean ticketOpenBundle,
            boolean discography
    ) {}
}
