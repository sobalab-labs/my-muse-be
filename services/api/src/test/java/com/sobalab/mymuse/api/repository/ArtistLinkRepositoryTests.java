package com.sobalab.mymuse.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sobalab.mymuse.api.model.enums.LinkPlatform;

class ArtistLinkRepositoryTests extends RepositoryQueryTestSupport {
    @Autowired
    ArtistLinkRepository repository;

    @Test
    void findByArtistIdReturnsOnlyThatArtistsLinksAndHandlesMissingLinks() {
        var artist = artist("Muse");
        var emptyArtist = artist("No links");
        var official = link(artist, LinkPlatform.OFFICIAL);
        var youtube = link(artist, LinkPlatform.YOUTUBE);
        link(artist("Other"), LinkPlatform.OFFICIAL);
        flushAndClear();

        assertThat(repository.findByArtistId(artist.getId())).extracting("id")
                .containsExactlyInAnyOrder(official.getId(), youtube.getId());
        assertThat(repository.findByArtistId(emptyArtist.getId())).isEmpty();
        assertThat(repository.findByArtistId(UUID.randomUUID())).isEmpty();
    }
}
