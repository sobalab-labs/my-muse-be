package com.sobalab.mymuse.api.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import com.sobalab.mymuse.api.dto.ArtistDto;
import com.sobalab.mymuse.api.model.Artist;
import com.sobalab.mymuse.api.repository.ArtistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArtistService {
    private final ArtistRepository artistRepository;

    @Transactional(readOnly = true)
    public ArtistDto getArtist(UUID artistId) {
        if (artistId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Artist ID is required");
        }
        Artist artist = artistRepository.findWithLinksById(artistId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artist not found: " + artistId));
        return ArtistDto.from(artist);
    }
}
