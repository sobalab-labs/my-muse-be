package com.sobalab.mymuse.api.dto;

import com.sobalab.mymuse.api.model.enums.Nation;
import java.util.List;

public record ArtistDto(
        String id,
        String name,
        String genre,
        Nation nation,
        List<ArtistSnsDto> snsUrls,
        String imageUrl
) {
    public record ArtistSnsDto(String platform, String url) {}
}
