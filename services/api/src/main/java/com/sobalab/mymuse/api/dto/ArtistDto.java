package com.sobalab.mymuse.api.dto;

import com.sobalab.mymuse.api.model.enums.Nation;
import java.util.List;

public record ArtistDto(
        String id,
        String name,
        String genre,
        Nation nation,
        List<ArtistLinkDto> snsUrls,
        String imageUrl
) {
    public record ArtistLinkDto(String platform, String url) {}
}
