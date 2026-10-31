package com.sobalab.mymuse.api.dto;

public record NotificationPreferencesDto(
        boolean channelEmailEnabled,
        boolean channelKakaoEnabled,
        boolean generalWeeklyUpdates,
        boolean artistNewConcertEnabled,
        boolean bookmarkedEventTicketOpenEnabled) {
}