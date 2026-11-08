package com.sobalab.mymuse.api.dto;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.sobalab.mymuse.api.model.Concert;

public record ConcertDto(
        String id,
        String title,
        OffsetDateTime date,
        String place,
        Integer durationMinutes,
        String ticketingUrl,
        String nation) {
    public static ConcertDto from(Concert concert) {
        return new ConcertDto(
                concert.getId().toString(),
                concert.getTitle(),
                concert.getDate() != null ? concert.getDate().atZone(ZoneId.systemDefault()).toOffsetDateTime() : null,
                concert.getVenue() != null ? concert.getVenue().getName() : null,
                concert.getDurationMinutes(),
                concert.getTicketingUrl(),
                concert.getVenue() != null && concert.getVenue().getNation() != null
                        ? concert.getVenue().getNation().name().toLowerCase()
                        : null);
    }
}
