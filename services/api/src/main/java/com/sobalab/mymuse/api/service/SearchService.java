package com.sobalab.mymuse.api.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sobalab.mymuse.api.dto.SearchResponse;
import com.sobalab.mymuse.api.dto.ArtistDto;
import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.dto.CursorResponse;
import com.sobalab.mymuse.api.model.enums.SearchType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final ArtistService artistService;
    private final ConcertService concertService;

    public SearchResponse search(String query, List<SearchType> types, Integer limit) {
        return search(query, types, limit, null, null);
    }

    public SearchResponse search(String query, List<SearchType> types, Integer limit, String artistCursor, String concertCursor) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Query parameter 'q' must not be blank.");
        }
        int pageSize = limit == null ? 20 : limit;
        if (pageSize < 1 || pageSize > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be between 1 and 100");
        }
        List<SearchType> requested = types == null || types.isEmpty()
                ? List.of(SearchType.ARTIST, SearchType.CONCERT) : types;
        CursorResponse<ArtistDto> artists = requested.contains(SearchType.ARTIST)
                ? artistService.search(query.strip(), artistCursor, pageSize)
                : emptyPage();
        CursorResponse<ConcertDto> concerts = requested.contains(SearchType.CONCERT)
                ? concertService.search(query.strip(), concertCursor, pageSize)
                : emptyPage();
        return new SearchResponse(artists, concerts);
    }
    private static <T> CursorResponse<T> emptyPage() { return new CursorResponse<>(List.of(), null, false); }
}
