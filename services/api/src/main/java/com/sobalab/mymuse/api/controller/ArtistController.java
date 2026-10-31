package com.sobalab.mymuse.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sobalab.mymuse.api.dto.ArtistDto;
import com.sobalab.mymuse.api.service.ArtistService;

@RestController
@RequestMapping("/artists")
public class ArtistController {

    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    // @GetMapping
    // public CursorResponse<ArtistDto> searchArtists(
    // @RequestParam(required = false) String q,
    // @RequestParam(required = false) String cursor,
    // @RequestParam(required = false) Integer limit) {
    // List<ArtistDto> items = artistService.searchArtists(q, cursor, limit);
    // return new CursorResponse<>(items, null, false);
    // }

    @GetMapping("/{artistId}")
    public ArtistDto getArtist(@PathVariable String artistId) {
        return artistService.getArtist(artistId);
    }
}
