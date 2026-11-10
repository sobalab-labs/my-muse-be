package com.sobalab.mymuse.api.model;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_like_concert")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBookmarkConcert {

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        private UUID userId;
        private UUID concertId;
    }

    @EmbeddedId
    private Id id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("concertId")
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    public static UserBookmarkConcert of(User user, Concert concert) {
        UserBookmarkConcert like = new UserBookmarkConcert();
        like.id = new Id(user.getId(), concert.getId());
        like.user = user;
        like.concert = concert;
        return like;
    }
}
