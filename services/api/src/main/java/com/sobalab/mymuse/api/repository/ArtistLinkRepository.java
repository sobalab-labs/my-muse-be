package com.sobalab.mymuse.api.repository;

import com.sobalab.mymuse.api.model.ArtistLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArtistLinkRepository extends JpaRepository<ArtistLink, UUID> {

    @Query("SELECT s FROM ArtistLink s WHERE s.artist.id = :artistId")
    List<ArtistLink> findByArtistId(@Param("artistId") UUID artistId);
}
