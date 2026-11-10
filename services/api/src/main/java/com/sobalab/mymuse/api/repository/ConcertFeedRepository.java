package com.sobalab.mymuse.api.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sobalab.mymuse.api.model.Concert;
import com.sobalab.mymuse.api.model.enums.CountryCode;

public interface ConcertFeedRepository {
    default List<Concert> findTrendingFeed(CountryCode nation, LocalDateTime startDate,
            LocalDateTime endDate, Sort sort, Limit limit) {
        return nation == null
                ? findTrendingFeedWithoutNation(startDate, endDate, sort, limit)
                : findTrendingFeedByNation(nation, startDate, endDate, sort, limit);
    }

    @Query("""
            SELECT c FROM Concert c JOIN FETCH c.venue v
            WHERE (CAST(:startDate AS LocalDateTime) IS NULL OR c.date >= :startDate)
              AND (CAST(:endDate AS LocalDateTime) IS NULL OR c.date < :endDate)
            """)
    List<Concert> findTrendingFeedWithoutNation(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, Sort sort, Limit limit);

    @Query("""
            SELECT c FROM Concert c JOIN FETCH c.venue v
            WHERE v.nation = :nation
              AND (CAST(:startDate AS LocalDateTime) IS NULL OR c.date >= :startDate)
              AND (CAST(:endDate AS LocalDateTime) IS NULL OR c.date < :endDate)
            """)
    List<Concert> findTrendingFeedByNation(@Param("nation") CountryCode nation, @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, Sort sort, Limit limit);

    default List<Concert> findTrendingFeedAfterDate(CountryCode nation, LocalDateTime startDate,
            LocalDateTime endDate, LocalDateTime cursorDate,
            UUID cursorId, Limit limit) {
        return nation == null
                ? findTrendingFeedAfterDateWithoutNation(startDate, endDate, cursorDate, cursorId, limit)
                : findTrendingFeedAfterDateByNation(nation, startDate, endDate, cursorDate, cursorId, limit);
    }

    @Query("""
            SELECT c FROM Concert c JOIN FETCH c.venue v
            WHERE c.date >= :startDate
              AND (CAST(:endDate AS LocalDateTime) IS NULL OR c.date < :endDate)
              AND (c.date > :cursorDate OR (c.date = :cursorDate AND c.id < :cursorId))
            ORDER BY c.date ASC, c.id DESC
            """)
    List<Concert> findTrendingFeedAfterDateWithoutNation(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, @Param("cursorDate") LocalDateTime cursorDate,
            @Param("cursorId") UUID cursorId, Limit limit);

    @Query("""
            SELECT c FROM Concert c JOIN FETCH c.venue v
            WHERE v.nation = :nation
              AND c.date >= :startDate
              AND (CAST(:endDate AS LocalDateTime) IS NULL OR c.date < :endDate)
              AND (c.date > :cursorDate OR (c.date = :cursorDate AND c.id < :cursorId))
            ORDER BY c.date ASC, c.id DESC
            """)
    List<Concert> findTrendingFeedAfterDateByNation(@Param("nation") CountryCode nation, @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, @Param("cursorDate") LocalDateTime cursorDate,
            @Param("cursorId") UUID cursorId, Limit limit);

    default List<Concert> findTrendingFeedAfterAnnouncement(CountryCode nation,
            LocalDateTime startDate,
            LocalDateTime endDate, Instant cursorCreatedAt,
            UUID cursorId, Limit limit) {
        return nation == null
                ? findTrendingFeedAfterAnnouncementWithoutNation(startDate, endDate, cursorCreatedAt, cursorId, limit)
                : findTrendingFeedAfterAnnouncementByNation(nation, startDate, endDate, cursorCreatedAt, cursorId, limit);
    }

    @Query("""
            SELECT c FROM Concert c JOIN FETCH c.venue v
            WHERE c.date >= :startDate
              AND (CAST(:endDate AS LocalDateTime) IS NULL OR c.date < :endDate)
              AND (c.createdAt < :cursorCreatedAt
                   OR (c.createdAt = :cursorCreatedAt AND c.id < :cursorId))
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<Concert> findTrendingFeedAfterAnnouncementWithoutNation(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId, Limit limit);

    @Query("""
            SELECT c FROM Concert c JOIN FETCH c.venue v
            WHERE v.nation = :nation
              AND c.date >= :startDate
              AND (CAST(:endDate AS LocalDateTime) IS NULL OR c.date < :endDate)
              AND (c.createdAt < :cursorCreatedAt
                   OR (c.createdAt = :cursorCreatedAt AND c.id < :cursorId))
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<Concert> findTrendingFeedAfterAnnouncementByNation(@Param("nation") CountryCode nation,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId, Limit limit);

    @Query("""
            SELECT c FROM Concert c JOIN FETCH c.venue
            WHERE EXISTS (
                SELECT 1 FROM ConcertArtist ca JOIN UserFollowedArtist f ON f.artist = ca.artist
                WHERE ca.concert = c AND f.user.id = :userId
            )
              AND (CAST(:startDate AS LocalDateTime) IS NULL OR c.date >= :startDate)
              AND (CAST(:endDate AS LocalDateTime) IS NULL OR c.date < :endDate)
              AND (CAST(:cursorDate AS LocalDateTime) IS NULL
                   OR c.date > :cursorDate OR (c.date = :cursorDate AND c.id < :cursorId))
            ORDER BY c.date ASC, c.id DESC
            """)
    List<Concert> findFollowedArtistsFeed(@Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("cursorDate") LocalDateTime cursorDate, @Param("cursorId") UUID cursorId, Limit limit);

    @Query("""
            SELECT c FROM Concert c JOIN FETCH c.venue
            WHERE EXISTS (
                SELECT 1 FROM ConcertArtist ca JOIN UserFollowedArtist f ON f.artist = ca.artist
                WHERE ca.concert = c AND f.user.id = :userId
            )
              AND (CAST(:startDate AS LocalDateTime) IS NULL OR c.date >= :startDate)
              AND (CAST(:endDate AS LocalDateTime) IS NULL OR c.date < :endDate)
              AND (CAST(:cursorCreatedAt AS Instant) IS NULL
                   OR c.createdAt < :cursorCreatedAt
                   OR (c.createdAt = :cursorCreatedAt AND c.id < :cursorId))
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<Concert> findFollowedArtistsFeedAfterAnnouncement(@Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("cursorCreatedAt") Instant cursorCreatedAt, @Param("cursorId") UUID cursorId, Limit limit);
}
