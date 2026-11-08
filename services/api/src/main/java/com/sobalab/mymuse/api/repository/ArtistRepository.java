package com.sobalab.mymuse.api.repository;

import com.sobalab.mymuse.api.model.Artist;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, UUID> {

    default List<Artist> searchArtists(String q, String cursorName, UUID cursorId, Limit limit) {
        if (q == null || q.isBlank()) {
            throw new IllegalArgumentException("Search keyword must not be blank");
        }
        if ((cursorName == null) != (cursorId == null)) {
            throw new IllegalArgumentException("Cursor requires both name and ID");
        }
        return searchArtistsByKeyword(q, cursorName, cursorId, limit);
    }

    @Query("""
            SELECT a FROM Artist a
            WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%'))
              AND (:cursorName IS NULL OR a.name > :cursorName
                   OR (a.name = :cursorName AND a.id < :cursorId))
            ORDER BY a.name ASC, a.id DESC
            """)
    List<Artist> searchArtistsByKeyword(@Param("q") String q,
            @Param("cursorName") String cursorName, @Param("cursorId") UUID cursorId, Limit limit);

    @EntityGraph(attributePaths = "links")
    Optional<Artist> findWithLinksById(UUID id);
}
