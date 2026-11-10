package com.sobalab.mymuse.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sobalab.mymuse.api.dto.SearchResponse;
import com.sobalab.mymuse.api.model.enums.SearchType;
import com.sobalab.mymuse.api.service.SearchService;

import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResponse search(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "artist,concert") List<@NotNull SearchType> types,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false) String artistCursor,
            @RequestParam(required = false) String concertCursor) {
        if (q == null || q.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query parameter 'q' must not be blank.");
        }
        return searchService.search(q, types, limit, artistCursor, concertCursor);
    }

    // @GetMapping("/artists")
    // public CursorResponse<ArtistDto> searchArtists(
    // @RequestParam String cursor,
    // @RequestParam(required = false, defaultValue = "10") Integer limit) {
    // return searchService.searchArtists(cursor, limit);
    // }

    // @GetMapping("/concerts")
    // public CursorResponse<ConcertDto> searchConcerts(
    // @RequestParam String cursor,
    // @RequestParam(required = false, defaultValue = "20") Integer limit) {
    // return searchService.searchConcerts(cursor, limit);
    // }
}
