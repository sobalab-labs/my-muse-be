package com.sobalab.mymuse.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sobalab.mymuse.api.model.UserLikeConcert;
import com.sobalab.mymuse.api.model.enums.CountryCode;

class UserLikeConcertRepositoryTests extends RepositoryQueryTestSupport {
    @Autowired
    UserLikeConcertRepository repository;

    @Test
    void insertIfAbsentReturnsAffectedRowsAndDeduplicatesOnlyTheExactPair() {
        var user = user("Target");
        var other = user("Other");
        var venue = venue(CountryCode.JP);
        var first = concert("First", venue, LocalDateTime.of(2030, 1, 1, 18, 0));
        var second = concert("Second", venue, first.getDate().plusDays(1));
        flushAndClear();

        assertThat(repository.insertIfAbsent(user.getId(), first.getId())).isOne();
        assertThat(repository.insertIfAbsent(user.getId(), first.getId())).isZero();
        assertThat(repository.insertIfAbsent(other.getId(), first.getId())).isOne();
        assertThat(repository.insertIfAbsent(user.getId(), second.getId())).isOne();
        flushAndClear();
        assertThat(em.createQuery("select l.id from UserLikeConcert l", UserLikeConcert.Id.class).getResultList())
                .containsExactlyInAnyOrder(new UserLikeConcert.Id(user.getId(), first.getId()),
                        new UserLikeConcert.Id(other.getId(), first.getId()),
                        new UserLikeConcert.Id(user.getId(), second.getId()));
    }

    @Test
    void deleteByUserIdAndConcertIdDeletesOnlyThePairAndAllowsRepeatedOrMissingDeletes() {
        var user = user("Target");
        var other = user("Other");
        var venue = venue(CountryCode.KR);
        var first = concert("First", venue, LocalDateTime.of(2030, 1, 1, 18, 0));
        var second = concert("Second", venue, first.getDate().plusDays(1));
        persist(UserLikeConcert.of(user, first));
        persist(UserLikeConcert.of(other, first));
        persist(UserLikeConcert.of(user, second));
        flushAndClear();

        repository.deleteByUserIdAndConcertId(user.getId(), first.getId());
        repository.deleteByUserIdAndConcertId(user.getId(), first.getId());
        repository.deleteByUserIdAndConcertId(UUID.randomUUID(), first.getId());
        repository.deleteByUserIdAndConcertId(user.getId(), UUID.randomUUID());
        flushAndClear();

        assertThat(em.createQuery("select l.id from UserLikeConcert l", UserLikeConcert.Id.class).getResultList())
                .containsExactlyInAnyOrder(new UserLikeConcert.Id(other.getId(), first.getId()),
                        new UserLikeConcert.Id(user.getId(), second.getId()));
    }
}
