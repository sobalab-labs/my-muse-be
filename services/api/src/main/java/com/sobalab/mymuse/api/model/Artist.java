package com.sobalab.mymuse.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sobalab.mymuse.api.model.enums.Nation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "artists")
@Getter
@Setter
public class Artist extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String genre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Nation nation;

    @Column(name = "image_url")
    private String imageUrl;

    @OneToMany(mappedBy = "artist", fetch = FetchType.LAZY)
    private List<ArtistLink> links = new ArrayList<>();

    public List<ArtistLink> getLinks() {
        return Collections.unmodifiableList(links);
    }
}
