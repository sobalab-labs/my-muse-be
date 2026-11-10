package com.sobalab.mymuse.api.dto;

public record SearchResponse(
    CursorResponse<ArtistDto> artists,
    CursorResponse<ConcertDto> concerts
) {}
