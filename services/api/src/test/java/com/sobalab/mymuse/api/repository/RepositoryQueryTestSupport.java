package com.sobalab.mymuse.api.repository;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import com.sobalab.mymuse.api.TestcontainersConfiguration;
import com.sobalab.mymuse.api.model.Artist;
import com.sobalab.mymuse.api.model.ArtistLink;
import com.sobalab.mymuse.api.model.Concert;
import com.sobalab.mymuse.api.model.ConcertArtist;
import com.sobalab.mymuse.api.model.User;
import com.sobalab.mymuse.api.model.UserFollowedArtist;
import com.sobalab.mymuse.api.model.Venue;
import com.sobalab.mymuse.api.model.enums.CountryCode;
import com.sobalab.mymuse.api.model.enums.LinkPlatform;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
abstract class RepositoryQueryTestSupport {
    @Autowired
    EntityManager em;

    User user(String name) {
        return persist(User.builder().name(name).build());
    }

    Artist artist(String name) {
        Artist artist = new Artist();
        artist.setName(name);
        artist.setNation(CountryCode.JP);
        return persist(artist);
    }

    ArtistLink link(Artist artist, LinkPlatform platform) {
        ArtistLink link = instantiate(ArtistLink.class);
        field(link, "artist", artist);
        field(link, "platform", platform);
        field(link, "url", "https://example.com/" + platform);
        return persist(link);
    }

    Venue venue(CountryCode nation) {
        Venue venue = instantiate(Venue.class);
        field(venue, "name", nation.name());
        field(venue, "nation", nation);
        return persist(venue);
    }

    Concert concert(String title, Venue venue, LocalDateTime date) {
        Concert concert = instantiate(Concert.class);
        field(concert, "title", title);
        field(concert, "venue", venue);
        field(concert, "date", date);
        field(concert, "officialAnnouncementUrl", "https://example.com/announcement");
        return persist(concert);
    }

    void follow(User user, Artist artist) {
        UserFollowedArtist follow = instantiate(UserFollowedArtist.class);
        field(follow, "id", new UserFollowedArtist.Id(user.getId(), artist.getId()));
        field(follow, "user", user);
        field(follow, "artist", artist);
        persist(follow);
    }

    void performer(Concert concert, Artist artist) {
        ConcertArtist performer = instantiate(ConcertArtist.class);
        field(performer, "id", new ConcertArtist.Id(concert.getId(), artist.getId()));
        field(performer, "concert", concert);
        field(performer, "artist", artist);
        persist(performer);
    }

    <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    void flushAndClear() {
        em.flush();
        em.clear();
    }

    static void field(Object target, String name, Object value) {
        ReflectionTestUtils.setField(target, name, value);
    }

    private static <T> T instantiate(Class<T> type) {
        return org.springframework.beans.BeanUtils.instantiateClass(type);
    }
}
