package com.sobalab.mymuse.api.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sobalab.mymuse.api.dto.ArtistDto;
import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.dto.CursorResponse;
import com.sobalab.mymuse.api.dto.SearchResponse;
import com.sobalab.mymuse.api.model.enums.SearchType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final ArtistService artistService;
    private final ConcertService concertService;

    public SearchResponse search(String query, List<SearchType> types, Integer limit) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Query parameter 'q' must not be blank.");
        }
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
