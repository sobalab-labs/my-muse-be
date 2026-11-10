package com.sobalab.mymuse.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sobalab.mymuse.api.model.Artist;
import com.sobalab.mymuse.api.model.ArtistLink;
import com.sobalab.mymuse.api.model.enums.CountryCode;
import com.sobalab.mymuse.api.model.enums.LinkPlatform;
import com.sobalab.mymuse.api.repository.ArtistRepository;

@ExtendWith(MockitoExtension.class)
class ArtistServiceTests {
    @Mock
    ArtistRepository artistRepository;
    @InjectMocks
    ArtistService artistService;

    @Test
    void detailIncludesMetadataAndLinks() {
        UUID id = UUID.randomUUID();
        Artist artist = mock(Artist.class);
        ArtistLink link = mock(ArtistLink.class);
        when(artist.getId()).thenReturn(id);
        when(artist.getName()).thenReturn("Artist");
        when(artist.getNation()).thenReturn(CountryCode.JP);
        when(artist.getLinks()).thenReturn(List.of(link));
        when(link.getPlatform()).thenReturn(LinkPlatform.OFFICIAL);
        when(link.getUrl()).thenReturn("https://example.com");
        when(artistRepository.findWithLinksById(id)).thenReturn(Optional.of(artist));
        var result = artistService.getArtist(id);
        assertThat(result.id()).isEqualTo(id.toString());
        assertThat(result.name()).isEqualTo("Artist");
        assertThat(result.nation()).isEqualTo(CountryCode.JP);
        assertThat(result.linkUrls()).singleElement().satisfies(url -> {
            assertThat(url.platform()).isEqualTo("official");
            assertThat(url.url()).isEqualTo("https://example.com");
        });
        verify(artistRepository).findWithLinksById(id);
        verifyNoMoreInteractions(artistRepository);
    }

    @Test
    void absentOptionalMetadataAndLinksAreSupported() {
        Artist artist = new Artist();
        org.springframework.test.util.ReflectionTestUtils.setField(artist, "id", UUID.randomUUID());
        artist.setName("Artist");
        artist.setNation(CountryCode.KR);
        when(artistRepository.findWithLinksById(artist.getId())).thenReturn(Optional.of(artist));
        var result = artistService.getArtist(artist.getId());
        assertThat(result.linkUrls()).isEmpty();
        assertThat(result.genre()).isNull();
        assertThat(result.imageUrl()).isNull();
    }

    @Test
    void missingArtistIsNotFound() {
        UUID id = UUID.randomUUID();
        when(artistRepository.findWithLinksById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> artistService.getArtist(id))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void nullIdIsRejectedBeforeDatabaseAccess() {
        assertThatThrownBy(() -> artistService.getArtist(null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(artistRepository);
    }
}
