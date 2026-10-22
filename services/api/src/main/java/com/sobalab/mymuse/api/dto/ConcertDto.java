package com.sobalab.mymuse.api.dto;

import java.time.OffsetDateTime;

public record ConcertDto(
        String id,
        String title,
        OffsetDateTime date,
        String place,
        Integer durationMinutes,
        String ticketingUrl,
        String region
) {}
