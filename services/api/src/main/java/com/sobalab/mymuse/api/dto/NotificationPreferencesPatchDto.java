package com.sobalab.mymuse.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.Valid;

public record NotificationPreferencesPatchDto(
        @Valid ChannelsPatch channels,
        @Valid TopicsPatch topics) {
    public record ChannelsPatch(Boolean email, Boolean kakao) {
    }

    public record TopicsPatch(
            Boolean generalWeeklyUpdates,
            Boolean artistNewConcert,
            Boolean bookmarkedEventTicketOpen) {
    }

    @JsonIgnore
    public boolean isEmpty() {
        boolean noChannels = channels == null
                || (channels.email() == null && channels.kakao() == null);
        boolean noTopics = topics == null
                || (topics.generalWeeklyUpdates() == null
                        && topics.artistNewConcert() == null
                        && topics.bookmarkedEventTicketOpen() == null);
        return noChannels && noTopics;
    }
}
