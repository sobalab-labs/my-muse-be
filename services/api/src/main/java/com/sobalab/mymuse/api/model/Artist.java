package com.sobalab.mymuse.api.model;

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
}
