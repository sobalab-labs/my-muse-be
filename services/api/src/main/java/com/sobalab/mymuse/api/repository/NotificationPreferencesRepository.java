package com.sobalab.mymuse.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sobalab.mymuse.api.model.NotificationPreferences;

@Repository
public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, UUID> {

    Optional<NotificationPreferences> findByUserId(UUID userId);

    @Modifying
    @Query("UPDATE NotificationPreferences n SET n.channelEmailEnabled = :email, n.channelKakaoEnabled = :kakao, n.generalWeeklyUpdates = :weekly, n.artistNewConcertEnabled = :artistNew, n.bookmarkedEventTicketOpenEnabled = :ticketOpen WHERE n.user.id = :userId")
    int updatePreferences(@Param("userId") UUID userId,
                          @Param("email") boolean email,
                          @Param("kakao") boolean kakao,
                          @Param("weekly") boolean weekly,
                          @Param("artistNew") boolean artistNew,
                          @Param("ticketOpen") boolean ticketOpen);
}
