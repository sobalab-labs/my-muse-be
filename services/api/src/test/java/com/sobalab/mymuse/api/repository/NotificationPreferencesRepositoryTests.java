package com.sobalab.mymuse.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.sobalab.mymuse.api.model.NotificationPreferences;

class NotificationPreferencesRepositoryTests extends RepositoryQueryTestSupport {
    @Autowired
    NotificationPreferencesRepository repository;

    @ParameterizedTest
    @CsvSource({ "true,false,true,false,true", "false,true,false,true,false" })
    void updatePreferencesReplacesEveryFlagOnlyForTheTargetUser(
            boolean email, boolean kakao, boolean weekly, boolean artistNew, boolean ticketOpen) {
        var target = user("Target");
        var other = user("Other");
        var preferences = persist(NotificationPreferences.builder().user(target)
                .channelEmailEnabled(!email).channelKakaoEnabled(!kakao).generalWeeklyUpdates(!weekly)
                .artistNewConcertEnabled(!artistNew).bookmarkedEventTicketOpenEnabled(!ticketOpen).build());
        var untouched = persist(NotificationPreferences.builder().user(other).build());
        flushAndClear();

        assertThat(repository.updatePreferences(target.getId(), email, kakao, weekly, artistNew, ticketOpen)).isOne();
        flushAndClear();

        assertThat(em.find(NotificationPreferences.class, preferences.getId()))
                .extracting(NotificationPreferences::isChannelEmailEnabled,
                        NotificationPreferences::isChannelKakaoEnabled,
                        NotificationPreferences::isGeneralWeeklyUpdates,
                        NotificationPreferences::isArtistNewConcertEnabled,
                        NotificationPreferences::isBookmarkedEventTicketOpenEnabled)
                .containsExactly(email, kakao, weekly, artistNew, ticketOpen);
        assertThat(em.find(NotificationPreferences.class, untouched.getId()))
                .extracting(NotificationPreferences::isChannelEmailEnabled,
                        NotificationPreferences::isChannelKakaoEnabled,
                        NotificationPreferences::isGeneralWeeklyUpdates,
                        NotificationPreferences::isArtistNewConcertEnabled,
                        NotificationPreferences::isBookmarkedEventTicketOpenEnabled)
                .containsExactly(false, false, false, false, false);
    }

    @Test
    void updatePreferencesReturnsZeroWhenUserOrPreferencesDoNotExist() {
        var withoutPreferences = user("No preferences");
        flushAndClear();
        assertThat(repository.updatePreferences(withoutPreferences.getId(), true, true, true, true, true)).isZero();
        assertThat(repository.updatePreferences(UUID.randomUUID(), true, true, true, true, true)).isZero();
    }
}
