package com.sobalab.mymuse.api.service;

import com.sobalab.mymuse.api.dto.ArtistDto;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;

@Service
public class ArtistService {
    public List<ArtistDto> listArtists(String query) {
        return Collections.emptyList();
    }

    public ArtistDto getArtist(String artistId) {
        return null;
    }
}
