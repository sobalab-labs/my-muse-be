package com.sobalab.mymuse.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.sobalab.mymuse.api.model.enums.CountryCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "artists")
@Getter
@Setter
public class Artist extends AuditedEntity {

    @Column(nullable = false)
    private String name;

    private String genre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "nation", columnDefinition = "country_code")
    private CountryCode nation;

    @Column(name = "image_url")
    private String imageUrl;

    @OneToMany(mappedBy = "artist", fetch = FetchType.LAZY)
    private List<ArtistLink> links = new ArrayList<>();

    public List<ArtistLink> getLinks() {
        return Collections.unmodifiableList(links);
    }
}
