package com.sobalab.mymuse.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;

import com.sobalab.mymuse.api.model.Concert;
import com.sobalab.mymuse.api.model.enums.CountryCode;

@DataJpaTest
class ConcertRepositoryTests extends RepositoryQueryTestSupport {
    @Autowired
    ConcertRepository repository;
    private static final LocalDateTime START = LocalDateTime.of(2030, 6, 1, 0, 0);
    private static final LocalDateTime END = START.plusDays(7);
    private static final Sort BY_DATE = Sort.by("date").ascending().and(Sort.by("id").descending());
    private static final Sort BY_ANNOUNCEMENT = Sort.by("createdAt", "id").descending();

    @ParameterizedTest
    @CsvSource(value = {
            "LiVe,NULL,Alpha Live|Beta Live",
            "a,JP,Alpha Live|Gamma Show",
            "live,JP,Alpha Live",
            "live,KR,Beta Live",
            "show,KR,''",
            "missing,NULL,''"
    }, nullValues = "NULL")
    void searchConcertsCombinesCaseInsensitiveTitleAndOptionalNationFilters(
            String query, CountryCode nation, String expected) {
        var japan = venue(CountryCode.JP);
        concert("Alpha Live", japan, START);
        concert("Beta Live", venue(CountryCode.KR), START);
        concert("Gamma Show", japan, START);
        flushAndClear();

        assertThat(repository.searchConcerts(query, nation, null, null, Limit.of(10)))
                .extracting(Concert::getTitle).containsExactly(titles(expected));
    }

    @Test
    void searchConcertsSeeksAfterTitleAndIdAfterFiltering() {
        var japan = venue(CountryCode.JP);
        var gamma = concert("Gamma", japan, START);
        concert("Alpha", japan, START);
        var beta = concert("Beta", japan, START);
        concert("Excluded", venue(CountryCode.KR), START);
        flushAndClear();

        assertThat(repository.searchConcerts("a", CountryCode.JP, null, null, Limit.of(2)))
                .extracting(Concert::getTitle).containsExactly("Alpha", "Beta");
        assertThat(repository.searchConcerts("a", CountryCode.JP, beta.getTitle(), beta.getId(), Limit.of(2)))
                .extracting(Concert::getTitle).containsExactly("Gamma");
        assertThat(repository.searchConcerts("a", CountryCode.JP, gamma.getTitle(), gamma.getId(), Limit.of(2))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void searchConcertsWithoutTextExecutesNoSql(String query) {
        var statistics = em.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            assertThatThrownBy(() -> repository.searchConcerts(query, CountryCode.JP, null, null, Limit.of(10)))
                    .isInstanceOfAny(IllegalArgumentException.class,
                            org.springframework.dao.InvalidDataAccessApiUsageException.class)
                    .hasMessageContaining("Search keyword must not be blank");
            assertThat(statistics.getPrepareStatementCount()).isZero();
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "false,false,Before|Start|Inside|End",
            "true,false,Start|Inside|End",
            "false,true,Before|Start|Inside",
            "true,true,Start|Inside"
    })
    void findTrendingFeedSupportsOpenBoundsWithInclusiveStartAndExclusiveEnd(
            boolean startBound, boolean endBound, String expected) {
        var venue = venue(CountryCode.JP);
        concert("Before", venue, START.minusSeconds(1));
        concert("Start", venue, START);
        concert("Inside", venue, START.plusDays(1));
        concert("End", venue, END);
        flushAndClear();

        var results = repository.findTrendingFeed(null, startBound ? START : null,
                endBound ? END : null, BY_DATE, Limit.of(10));
        assertThat(results).extracting(Concert::getTitle).containsExactly(titles(expected));
        assertVenuesLoaded(results);
    }

    @Test
    void findTrendingFeedFiltersNationAndPaginatesAndHandlesEmptyRanges() {
        var japan = venue(CountryCode.JP);
        var first = concert("First", japan, START);
        var second = concert("Second", japan, START.plusDays(1));
        concert("Other nation", venue(CountryCode.KR), START);
        flushAndClear();

        assertThat(repository.findTrendingFeed(CountryCode.JP, START, END, BY_DATE, Limit.of(1)))
                .extracting(Concert::getId).containsExactly(first.getId());
        assertThat(repository.findTrendingFeedAfterDate(CountryCode.JP, START, END, first.getDate(), first.getId(), Limit.of(1)))
                .extracting(Concert::getId).containsExactly(second.getId());
        assertThat(repository.findTrendingFeedAfterDate(CountryCode.JP, START, END, second.getDate(), second.getId(), Limit.of(1))).isEmpty();
        assertThat(repository.findTrendingFeed(null, null, null, BY_DATE, Limit.of(10))).hasSize(3);
        assertThat(repository.findTrendingFeed(null, START, START, BY_DATE, Limit.of(10))).isEmpty();
        assertThat(repository.findTrendingFeed(null, END, START, BY_DATE, Limit.of(10))).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "true,true", "true,false", "false,true", "false,false" })
    void findTrendingFeedAfterDateUsesStrictCursorAndUuidTieBreakWithOptionalFilters(
            boolean filterNation, boolean filterEnd) {
        var japan = venue(CountryCode.JP);
        // UUID textual order matches PostgreSQL's unsigned UUID ordering, unlike
        // UUID.compareTo.
        var tied = List.of(concert("Tie A", japan, START), concert("Tie B", japan, START),
                concert("Tie C", japan, START)).stream()
                .sorted(Comparator.comparing(c -> c.getId().toString())).toList();
        var cursor = tied.get(1);
        var later = concert("Later", japan, START.plusDays(1));
        var otherNation = concert("Other", venue(CountryCode.KR), START.plusDays(2));
        var atEnd = concert("End", japan, END);
        concert("Before", japan, START.minusSeconds(1));
        flushAndClear();
        CountryCode nation = filterNation ? CountryCode.JP : null;
        LocalDateTime end = filterEnd ? END : null;
        var expected = new java.util.ArrayList<UUID>();
        expected.add(tied.getFirst().getId());
        expected.add(later.getId());
        if (!filterNation)
            expected.add(otherNation.getId());
        if (!filterEnd)
            expected.add(atEnd.getId());

        var results = repository.findTrendingFeedAfterDate(nation, START, end,
                cursor.getDate(), cursor.getId(), Limit.of(10));
        assertThat(results).extracting(Concert::getId).containsExactlyElementsOf(expected);
        assertVenuesLoaded(results);
        assertThat(repository.findTrendingFeedAfterDate(nation, START, end,
                cursor.getDate(), cursor.getId(), Limit.of(1)))
                .extracting(Concert::getId).containsExactly(tied.getFirst().getId());
        assertThat(repository.findTrendingFeedAfterDate(nation, START, end,
                END.plusDays(1), cursor.getId(), Limit.of(10))).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "true,true", "true,false", "false,true", "false,false" })
    void findTrendingFeedAfterAnnouncementUsesStrictCursorAndUuidTieBreakWithOptionalFilters(
            boolean filterNation, boolean filterEnd) {
        var japan = venue(CountryCode.JP);
        var tied = List.of(concert("Tie A", japan, START), concert("Tie B", japan, START),
                concert("Tie C", japan, START)).stream()
                .sorted(Comparator.comparing(c -> c.getId().toString())).toList();
        var cursor = tied.get(1);
        var older = concert("Older", japan, START.plusDays(1));
        var otherNation = concert("Other", venue(CountryCode.KR), START);
        var atEnd = concert("End", japan, END);
        var beforeStart = concert("Before start", japan, START.minusSeconds(1));
        var newer = concert("Newer", japan, START);
        em.flush();
        Instant announcement = Instant.parse("2029-01-01T00:00:00Z");
        tied.forEach(c -> announcementAt(c, announcement));
        announcementAt(older, announcement.minusSeconds(1));
        announcementAt(otherNation, announcement.minusSeconds(2));
        announcementAt(atEnd, announcement.minusSeconds(3));
        announcementAt(beforeStart, announcement.minusSeconds(4));
        announcementAt(newer, announcement.plusSeconds(1));
        em.clear();
        CountryCode nation = filterNation ? CountryCode.JP : null;
        LocalDateTime end = filterEnd ? END : null;
        var expected = new java.util.ArrayList<UUID>();
        expected.add(tied.getFirst().getId());
        expected.add(older.getId());
        if (!filterNation)
            expected.add(otherNation.getId());
        if (!filterEnd)
            expected.add(atEnd.getId());

        var results = repository.findTrendingFeedAfterAnnouncement(nation, START, end,
                announcement, cursor.getId(), Limit.of(10));
        assertThat(results).extracting(Concert::getId).containsExactlyElementsOf(expected);
        assertVenuesLoaded(results);
        assertThat(repository.findTrendingFeedAfterAnnouncement(nation, START, end,
                announcement, cursor.getId(), Limit.of(1)))
                .extracting(Concert::getId).containsExactly(tied.getFirst().getId());
        assertThat(repository.findTrendingFeedAfterAnnouncement(nation, START, end,
                announcement.minusSeconds(10), cursor.getId(), Limit.of(10))).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "false,false,Before|Start|Inside|End",
            "true,false,Start|Inside|End",
            "false,true,Before|Start|Inside",
            "true,true,Start|Inside"
    })
    void findFollowedArtistsFeedDeduplicatesConcertsScopesUserAndAppliesDateBounds(
            boolean startBound, boolean endBound, String expected) {
        var target = user("Target");
        var firstArtist = artist("First artist");
        var secondArtist = artist("Second artist");
        var unrelated = artist("Unrelated");
        follow(target, firstArtist);
        follow(target, secondArtist);
        follow(user("Other"), unrelated);
        var venue = venue(CountryCode.JP);
        var before = concert("Before", venue, START.minusSeconds(1));
        var start = concert("Start", venue, START);
        var inside = concert("Inside", venue, START.plusDays(1));
        var end = concert("End", venue, END);
        for (var concert : List.of(before, start, inside, end))
            performer(concert, firstArtist);
        performer(start, secondArtist);
        performer(concert("Other user's artist", venue, START), unrelated);
        concert("No performers", venue, START);
        flushAndClear();
        LocalDateTime from = startBound ? START : null;
        LocalDateTime to = endBound ? END : null;

        assertThat(repository.findFollowedArtistsFeed(target.getId(), from, to, null, null, Limit.of(10)))
                .extracting(Concert::getTitle).containsExactly(titles(expected));
        assertThat(repository.findFollowedArtistsFeed(target.getId(), from, to, null, null, Limit.of(1)))
                .extracting(Concert::getTitle).containsExactly(titles(expected)[0]);
        assertThat(repository.findFollowedArtistsFeed(target.getId(), from, to,
                startBound ? start.getDate() : before.getDate(),
                startBound ? start.getId() : before.getId(), Limit.of(1)))
                .extracting(Concert::getTitle).containsExactly(titles(expected)[1]);
        assertThat(repository.findFollowedArtistsFeed(UUID.randomUUID(), from, to, null, null, Limit.of(10))).isEmpty();
        assertThat(repository.findFollowedArtistsFeed(target.getId(), START, START, null, null, Limit.of(10))).isEmpty();
        assertThat(repository.findFollowedArtistsFeed(target.getId(), END, START, null, null, Limit.of(10))).isEmpty();
    }

    @Test
    void followedAnnouncementFeedSeeksWithUuidTieBreakAndScopesUser() {
        var target = user("Target");
        var followed = artist("Followed");
        follow(target, followed);
        var venue = venue(CountryCode.JP);
        var tied = java.util.stream.Stream.of(concert("A", venue, START), concert("B", venue, START),
                concert("C", venue, START))
                .sorted(Comparator.comparing(c -> c.getId().toString(), Comparator.reverseOrder())).toList();
        tied.forEach(c -> performer(c, followed));
        var older = concert("Older", venue, START);
        performer(older, followed);
        concert("Unrelated", venue, START);
        em.flush();
        var created = Instant.parse("2029-01-01T00:00:00Z");
        tied.forEach(c -> announcementAt(c, created));
        announcementAt(older, created.minusSeconds(1));
        em.clear();
        assertThat(repository.findFollowedArtistsFeedAfterAnnouncement(target.getId(), null, null,
                null, null, Limit.of(1))).extracting(Concert::getId).containsExactly(tied.getFirst().getId());
        assertThat(repository.findFollowedArtistsFeedAfterAnnouncement(target.getId(), null, null,
                created, tied.getFirst().getId(), Limit.of(10))).extracting(Concert::getId)
                .containsExactly(tied.get(1).getId(), tied.get(2).getId(), older.getId());
    }

    private void announcementAt(Concert concert, Instant time) {
        em.createNativeQuery("update concerts set created_at = :time where id = :id")
                .setParameter("time", time).setParameter("id", concert.getId()).executeUpdate();
    }

    private void assertVenuesLoaded(List<Concert> concerts) {
        var persistence = em.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(concerts).allSatisfy(c -> assertThat(persistence.isLoaded(c, "venue")).isTrue());
    }

    private static String[] titles(String expected) {
        return expected.isEmpty() ? new String[0] : expected.split("\\|");
    }
}
