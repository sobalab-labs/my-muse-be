package com.sobalab.mymuse.api.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sobalab.mymuse.api.dto.ArtistDto;
import com.sobalab.mymuse.api.model.Artist;
import com.sobalab.mymuse.api.model.ArtistSNS;
import com.sobalab.mymuse.api.repository.ArtistRepository;
import com.sobalab.mymuse.api.repository.ArtistSnsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArtistService {
    private final ArtistRepository artistRepository;
    private final ArtistSnsRepository artistSnsRepository;

    // @Transactional(readOnly = true)
    // public List<ArtistDto> searchArtists(String query, String cursor, Integer
    // limit) {
    // int pageSize = (limit != null && limit > 0) ? limit : 20;
    // PageRequest pageRequest = PageRequest.of(0, pageSize,
    // Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC,
    // "id")));
    // List<Artist> artists = artistRepository.searchArtists(query, pageRequest);
    // return artists.stream().map(toDto).collect(Collectors.toList());
    // }

    @Transactional(readOnly = true)
    public ArtistDto getArtist(String artistId) {
        UUID uuid = UUID.fromString(artistId);
        Artist artist = artistRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Artist not found: " + artistId));
        return toDto.apply(artist);
    }

    private final java.util.function.Function<Artist, ArtistDto> toDto = artist -> {
        List<ArtistSNS> snsList = artistSnsRepository.findByArtistId(artist.getId());
        List<ArtistDto.ArtistSnsDto> snsDtos = snsList.stream()
                .map(sns -> new ArtistDto.ArtistSnsDto(sns.getPlatform().name().toLowerCase(), sns.getUrl()))
                .collect(Collectors.toList());
        return new ArtistDto(
                artist.getId().toString(),
                artist.getName(),
                artist.getGenre(),
                artist.getNation(),
                snsDtos,
                artist.getImageUrl());
    };
}
