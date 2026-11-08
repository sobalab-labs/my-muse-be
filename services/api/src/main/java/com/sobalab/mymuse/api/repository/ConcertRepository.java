package com.sobalab.mymuse.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sobalab.mymuse.api.model.Concert;
import com.sobalab.mymuse.api.model.enums.CountryCode;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, UUID>, ConcertFeedRepository {

    @Override
    @EntityGraph(attributePaths = "venue")
    Optional<Concert> findById(UUID id);

    default List<Concert> searchConcerts(String q, CountryCode nation, String cursorTitle, UUID cursorId, Limit limit) {
        if (q == null || q.isBlank()) {
            throw new IllegalArgumentException("Search keyword must not be blank");
        }
        if ((cursorTitle == null) != (cursorId == null)) {
            throw new IllegalArgumentException("Cursor requires both title and ID");
        }
        return nation == null
                ? searchConcertsByKeyword(q, cursorTitle, cursorId, limit)
                : searchConcertsByKeywordAndNation(q, nation, cursorTitle, cursorId, limit);
    }

    @Query("""
            SELECT c FROM Concert c JOIN c.venue v
            WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%'))
              AND (:cursorTitle IS NULL OR c.title > :cursorTitle
                   OR (c.title = :cursorTitle AND c.id < :cursorId))
            ORDER BY c.title ASC, c.id DESC
            """)
    List<Concert> searchConcertsByKeyword(@Param("q") String q,
            @Param("cursorTitle") String cursorTitle, @Param("cursorId") UUID cursorId, Limit limit);

    @Query("""
            SELECT c FROM Concert c JOIN c.venue v
            WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%'))
              AND v.nation = :nation
              AND (:cursorTitle IS NULL OR c.title > :cursorTitle
                   OR (c.title = :cursorTitle AND c.id < :cursorId))
            ORDER BY c.title ASC, c.id DESC
            """)
    List<Concert> searchConcertsByKeywordAndNation(@Param("q") String q, @Param("nation") CountryCode nation,
            @Param("cursorTitle") String cursorTitle, @Param("cursorId") UUID cursorId, Limit limit);
}
