package com.sobalab.mymuse.api.dto;

import com.sobalab.mymuse.api.model.NotificationPreferences;

public record NotificationPreferencesDto(
        boolean channelEmailEnabled,
        boolean channelKakaoEnabled,
        boolean generalWeeklyUpdates,
        boolean artistNewConcertEnabled,
        boolean bookmarkedEventTicketOpenEnabled) {

    public static NotificationPreferencesDto from(NotificationPreferences prefs) {
        if (prefs == null) {
            return new NotificationPreferencesDto(false, false, false, false, false);
        }
        return new NotificationPreferencesDto(
                prefs.isChannelEmailEnabled(),
                prefs.isChannelKakaoEnabled(),
                prefs.isGeneralWeeklyUpdates(),
                prefs.isArtistNewConcertEnabled(),
                prefs.isBookmarkedEventTicketOpenEnabled());
    }
}
