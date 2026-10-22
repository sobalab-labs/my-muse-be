package com.sobalab.mymuse.api.model;

import com.sobalab.mymuse.api.model.enums.SNSPlatform;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "artist_sns")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistSNS extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SNSPlatform platform;

    @Column(nullable = false)
    private String url;
}
