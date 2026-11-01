package com.sobalab.mymuse.api.service;

import org.springframework.stereotype.Service;

import com.sobalab.mymuse.api.dto.ArtistDto;
import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.dto.CursorResponse;
import com.sobalab.mymuse.api.dto.SearchResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final ArtistService artistService;
    private final ConcertService concertService;

    public SearchResponse search(String query, String types, Integer limit) {
        // TODO: Implement
        return null;
    }

    public CursorResponse<ArtistDto> searchArtists(String cursor, Integer limit) {
        // TODO: Implement
        return null;
    }

    public CursorResponse<ConcertDto> searchConcerts(String cursor, Integer limit) {
        // TODO: Implement
        return null;
    }
}
