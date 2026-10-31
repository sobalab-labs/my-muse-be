package com.sobalab.mymuse.api.repository;

import com.sobalab.mymuse.api.model.Artist;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, UUID> {

    @Query("SELECT a FROM Artist a WHERE (:q IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Artist> searchArtists(@Param("q") String q, Pageable pageable);
}
