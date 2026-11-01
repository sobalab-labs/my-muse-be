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

    @GetMapping("/{artistId}")
    public ArtistDto getArtist(@PathVariable String artistId) {
        return artistService.getArtist(artistId);
    }
}
