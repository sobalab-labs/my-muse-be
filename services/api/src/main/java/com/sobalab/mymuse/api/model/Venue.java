package com.sobalab.mymuse.api.model;

import com.sobalab.mymuse.api.model.enums.Region;
import com.sobalab.mymuse.api.model.enums.VenueType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "venues")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venue extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VenueType type = VenueType.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region region;
}
