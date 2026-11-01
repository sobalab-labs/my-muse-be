package com.sobalab.mymuse.api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
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
    public ArtistDto getArtist(String artistId) {
        UUID uuid = UUID.fromString(artistId);
        Artist artist = artistRepository.findWithLinksById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Artist not found: " + artistId));
        return toDto(artist);
    }

    private ArtistDto toDto(Artist artist) {
        List<ArtistDto.ArtistLinkDto> linkDtos = artist.getLinks().stream()
                .map(link -> new ArtistDto.ArtistLinkDto(
                        link.getPlatform().name().toLowerCase(),
                        link.getUrl()))
                .toList();

        return new ArtistDto(
                artist.getId().toString(),
                artist.getName(),
                artist.getGenre(),
                artist.getNation(),
                linkDtos,
                artist.getImageUrl());
    }
}
