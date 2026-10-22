package com.sobalab.mymuse.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "user_like_concert")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLikeConcert {

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
}
