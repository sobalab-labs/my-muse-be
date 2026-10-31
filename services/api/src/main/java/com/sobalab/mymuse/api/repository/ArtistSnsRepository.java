package com.sobalab.mymuse.api.repository;

import com.sobalab.mymuse.api.model.ArtistSNS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArtistSnsRepository extends JpaRepository<ArtistSNS, UUID> {

    @Query("SELECT s FROM ArtistSNS s WHERE s.artist.id = :artistId")
    List<ArtistSNS> findByArtistId(@Param("artistId") UUID artistId);
}
