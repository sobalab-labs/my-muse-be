package com.sobalab.mymuse.api.service;

import java.util.UUID;
import java.util.List;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import org.springframework.data.domain.Limit;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import com.sobalab.mymuse.api.dto.ArtistDto;
import com.sobalab.mymuse.api.model.Artist;
import com.sobalab.mymuse.api.repository.ArtistRepository;
import com.sobalab.mymuse.api.dto.CursorResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArtistService {
    private final ArtistRepository artistRepository;

    @Transactional(readOnly = true)
    public CursorResponse<ArtistDto> search(String query, String cursor, int pageSize) {
        String name = null;
        UUID id = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\n", 2);
                if (parts.length != 2) throw new IllegalArgumentException();
                name = parts[0]; id = UUID.fromString(parts[1]);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid artist search cursor");
            }
        }
        List<ArtistDto> rows = artistRepository.searchArtists(query, name, id, Limit.of(pageSize + 1))
                .stream().map(ArtistDto::from).toList();
        boolean hasNext = rows.size() > pageSize;
        List<ArtistDto> items = hasNext ? rows.subList(0, pageSize) : rows;
        String next = hasNext ? Base64.getUrlEncoder().withoutPadding().encodeToString(
                (items.get(items.size() - 1).name() + "\n" + items.get(items.size() - 1).id()).getBytes(StandardCharsets.UTF_8)) : null;
        return new CursorResponse<>(items, next, hasNext);
    }

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
