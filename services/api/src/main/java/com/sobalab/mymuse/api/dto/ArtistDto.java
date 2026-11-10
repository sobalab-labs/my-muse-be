package com.sobalab.mymuse.api.dto;

import java.util.List;

import com.sobalab.mymuse.api.model.Artist;
import com.sobalab.mymuse.api.model.ArtistLink;
import com.sobalab.mymuse.api.model.enums.CountryCode;

public record ArtistDto(
        String id,
        String name,
        String genre,
        CountryCode nation,
        List<ArtistLinkDto> linkUrls,
        String imageUrl) {
    public record ArtistLinkDto(String platform, String url) {
        public static ArtistLinkDto from(ArtistLink link) {
            return new ArtistLinkDto(
                    link.getPlatform().getValue(),
                    link.getUrl());
        }
    }

    public static ArtistDto from(Artist artist) {
        List<ArtistLinkDto> linkDtos = artist.getLinks() != null
                ? artist.getLinks().stream().map(ArtistLinkDto::from).toList()
                : List.of();
        return new ArtistDto(
                artist.getId().toString(),
                artist.getName(),
                artist.getGenre(),
                artist.getNation(),
                linkDtos,
                artist.getImageUrl());
    }
}
