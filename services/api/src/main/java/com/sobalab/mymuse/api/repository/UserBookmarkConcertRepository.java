package com.sobalab.mymuse.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sobalab.mymuse.api.model.UserBookmarkConcert;

@Repository
public interface UserBookmarkConcertRepository extends JpaRepository<UserBookmarkConcert, UserBookmarkConcert.Id> {

    List<UserBookmarkConcert> findByUserId(UUID userId);

    boolean existsByUserIdAndConcertId(UUID userId, UUID concertId);

    @Modifying
    @Query(value = """
            INSERT INTO user_like_concert (user_id, concert_id)
            VALUES (:userId, :concertId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("concertId") UUID concertId);

    @Modifying
    @Query("DELETE FROM UserBookmarkConcert l WHERE l.user.id = :userId AND l.concert.id = :concertId")
    void deleteByUserIdAndConcertId(@Param("userId") UUID userId, @Param("concertId") UUID concertId);
}
