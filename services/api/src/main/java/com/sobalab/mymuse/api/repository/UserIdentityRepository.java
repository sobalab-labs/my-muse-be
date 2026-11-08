package com.sobalab.mymuse.api.repository;

import com.sobalab.mymuse.api.model.UserIdentity;
import com.sobalab.mymuse.api.model.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {
    @EntityGraph(attributePaths = "user")
    Optional<UserIdentity> findByProviderAndProviderSubject(OAuthProvider provider, String providerSubject);
}
