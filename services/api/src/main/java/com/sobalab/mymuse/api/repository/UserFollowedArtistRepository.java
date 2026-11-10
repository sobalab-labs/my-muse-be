package com.sobalab.mymuse.api.repository;

import com.sobalab.mymuse.api.model.UserFollowedArtist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserFollowedArtistRepository extends JpaRepository<UserFollowedArtist, UserFollowedArtist.Id> {

    List<UserFollowedArtist> findByUserId(UUID userId);

    @Query("SELECT f FROM UserFollowedArtist f JOIN FETCH f.artist a LEFT JOIN FETCH a.links WHERE f.user.id = :userId")
    List<UserFollowedArtist> findWithArtistsByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndArtistId(UUID userId, UUID artistId);

    @Modifying
    @Query(value = "INSERT INTO user_followed_artist (user_id, artist_id) VALUES (:userId, :artistId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void insertFollow(@Param("userId") UUID userId, @Param("artistId") UUID artistId);

    @Modifying
    @Query("DELETE FROM UserFollowedArtist f WHERE f.user.id = :userId AND f.artist.id = :artistId")
    void deleteByUserIdAndArtistId(@Param("userId") UUID userId, @Param("artistId") UUID artistId);
}
