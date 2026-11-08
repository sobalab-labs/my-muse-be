package com.sobalab.mymuse.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;

class ArtistRepositoryTests extends RepositoryQueryTestSupport {
    @Autowired
    ArtistRepository repository;

    @Test
    void searchArtistsMatchesCaseInsensitiveSubstringsAndReturnsEmptyForNoMatch() {
        var muse = artist("The MUSE Band");
        artist("Radiohead");
        flushAndClear();

        assertThat(repository.searchArtists("mUsE", null, null, Limit.of(10)))
                .extracting("id").containsExactly(muse.getId());
        assertThat(repository.searchArtists("unknown", null, null, Limit.of(10))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void searchArtistsWithoutTextExecutesNoSql(String query) {
        var statistics = em.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            assertThatThrownBy(() -> repository.searchArtists(query, null, null, Limit.of(10)))
                    .isInstanceOfAny(IllegalArgumentException.class,
                            org.springframework.dao.InvalidDataAccessApiUsageException.class)
                    .hasMessageContaining("Search keyword must not be blank");
            assertThat(statistics.getPrepareStatementCount()).isZero();
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    @Test
    void searchArtistsUsesUuidTieBreakAndDoesNotRepeatRowsAfterEarlierInsert() {
        var tied = java.util.stream.Stream.of(artist("Same"), artist("Same"), artist("Same"))
                .sorted(java.util.Comparator.comparing(a -> a.getId().toString(), java.util.Comparator.reverseOrder()))
                .toList();
        flushAndClear();
        var first = repository.searchArtists("a", null, null, Limit.of(1)).getFirst();
        assertThat(first.getId()).isEqualTo(tied.getFirst().getId());
        artist("Aardvark");
        flushAndClear();
        assertThat(repository.searchArtists("a", first.getName(), first.getId(), Limit.of(10)))
                .extracting("id").containsExactly(tied.get(1).getId(), tied.get(2).getId());
    }

    @Test
    void searchArtistsSeeksAfterNameAndId() {
        String query = "a";
        var beta = artist("Beta");
        var alpha = artist("Alpha");
        var gamma = artist("Gamma");
        flushAndClear();

        assertThat(repository.searchArtists(query, null, null, Limit.of(2)))
                .extracting("id").containsExactly(alpha.getId(), beta.getId());
        assertThat(repository.searchArtists(query, beta.getName(), beta.getId(), Limit.of(2)))
                .extracting("id").containsExactly(gamma.getId());
        assertThat(repository.searchArtists(query, gamma.getName(), gamma.getId(), Limit.of(2))).isEmpty();
    }
}
