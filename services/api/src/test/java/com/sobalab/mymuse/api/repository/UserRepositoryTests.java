package com.sobalab.mymuse.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sobalab.mymuse.api.model.User;

class UserRepositoryTests extends RepositoryQueryTestSupport {
    @Autowired
    UserRepository repository;

    @Test
    void completeOnboardingUpdatesOnlyTheTargetAndIsIdempotent() {
        var target = user("Target");
        var other = user("Other");
        flushAndClear();

        repository.completeOnboarding(target.getId());
        flushAndClear();
        assertThat(em.find(User.class, target.getId()).isOnboardingCompleted()).isTrue();
        assertThat(em.find(User.class, other.getId()).isOnboardingCompleted()).isFalse();

        repository.completeOnboarding(target.getId());
        repository.completeOnboarding(UUID.randomUUID());
        flushAndClear();
        assertThat(em.find(User.class, target.getId()).isOnboardingCompleted()).isTrue();
        assertThat(em.find(User.class, other.getId()).isOnboardingCompleted()).isFalse();
    }
}
