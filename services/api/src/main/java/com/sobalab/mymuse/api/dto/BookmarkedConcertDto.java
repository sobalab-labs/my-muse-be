package com.sobalab.mymuse.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookmarkedConcertDto(
        UUID id,
        UUID userId,
        UUID concertId,
        OffsetDateTime createdAt
) {}
