package com.sobalab.mymuse.api.repository;

import com.sobalab.mymuse.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Modifying
    @Query("UPDATE User u SET u.onboardingCompleted = true WHERE u.id = :userId")
    void completeOnboarding(@Param("userId") UUID userId);
}
