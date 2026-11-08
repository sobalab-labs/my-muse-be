package com.sobalab.mymuse.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sobalab.mymuse.api.model.UserFollowedArtist;
import com.sobalab.mymuse.api.model.enums.LinkPlatform;

import jakarta.persistence.PersistenceUnitUtil;

class UserFollowedArtistRepositoryTests extends RepositoryQueryTestSupport {
    @Autowired
    UserFollowedArtistRepository repository;

    @Test
    void findWithArtistsByUserIdFetchesArtistsAndLinksWithoutDroppingOrDuplicatingFollows() {
        var user = user("Target");
        var linked = artist("Linked");
        var unlinked = artist("Unlinked");
        link(linked, LinkPlatform.OFFICIAL);
        link(linked, LinkPlatform.YOUTUBE);
        follow(user, linked);
        follow(user, unlinked);
        follow(user("Other"), artist("Excluded"));
        flushAndClear();

        var results = repository.findWithArtistsByUserId(user.getId());
        assertThat(results).extracting(f -> f.getArtist().getId())
                .containsExactlyInAnyOrder(linked.getId(), unlinked.getId());
        PersistenceUnitUtil persistence = em.getEntityManagerFactory().getPersistenceUnitUtil();
        for (var result : results) {
            assertThat(persistence.isLoaded(result, "artist")).isTrue();
            assertThat(persistence.isLoaded(result.getArtist(), "links")).isTrue();
        }
        em.clear();
        assertThat(results.stream().filter(f -> f.getArtist().getId().equals(linked.getId()))
                .findFirst().orElseThrow().getArtist().getLinks()).hasSize(2);
        assertThat(repository.findWithArtistsByUserId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void insertFollowIgnoresDuplicatePairsAndAllowsIndependentPairs() {
        var user = user("Target");
        var other = user("Other");
        var artist = artist("Muse");
        var second = artist("Radiohead");
        flushAndClear();

        repository.insertFollow(user.getId(), artist.getId());
        repository.insertFollow(user.getId(), artist.getId());
        repository.insertFollow(other.getId(), artist.getId());
        repository.insertFollow(user.getId(), second.getId());
        flushAndClear();

        assertThat(em.createQuery("select f.id from UserFollowedArtist f", UserFollowedArtist.Id.class).getResultList())
                .containsExactlyInAnyOrder(new UserFollowedArtist.Id(user.getId(), artist.getId()),
                        new UserFollowedArtist.Id(other.getId(), artist.getId()),
                        new UserFollowedArtist.Id(user.getId(), second.getId()));
    }

    @Test
    void deleteByUserIdAndArtistIdDeletesOnlyThePairAndAllowsRepeatedOrMissingDeletes() {
        var user = user("Target");
        var other = user("Other");
        var artist = artist("Muse");
        var second = artist("Radiohead");
        follow(user, artist);
        follow(other, artist);
        follow(user, second);
        flushAndClear();

        repository.deleteByUserIdAndArtistId(user.getId(), artist.getId());
        repository.deleteByUserIdAndArtistId(user.getId(), artist.getId());
        repository.deleteByUserIdAndArtistId(UUID.randomUUID(), artist.getId());
        repository.deleteByUserIdAndArtistId(user.getId(), UUID.randomUUID());
        flushAndClear();

        assertThat(em.createQuery("select f.id from UserFollowedArtist f", UserFollowedArtist.Id.class).getResultList())
                .containsExactlyInAnyOrder(new UserFollowedArtist.Id(other.getId(), artist.getId()),
                        new UserFollowedArtist.Id(user.getId(), second.getId()));
    }
}
