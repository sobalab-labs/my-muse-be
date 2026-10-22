package com.sobalab.mymuse.api.controller;

import com.sobalab.mymuse.api.dto.ArtistDto;
import com.sobalab.mymuse.api.service.ArtistService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/artists")
public class ArtistController {

    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    @GetMapping
    public List<ArtistDto> listArtists(@RequestParam(required = false) String q) {
        return artistService.listArtists(q);
    }

    @GetMapping("/{artistId}")
    public ArtistDto getArtist(@PathVariable String artistId) {
        return artistService.getArtist(artistId);
    }
}
