package com.sobalab.mymuse.api.dto;

import java.util.UUID;

public record BookmarkedConcertDto(
        UUID userId,
        UUID concertId
) {}
