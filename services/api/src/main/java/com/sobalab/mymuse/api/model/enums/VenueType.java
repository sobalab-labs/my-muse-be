package com.sobalab.mymuse.api.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.EnumeratedValue;

public enum VenueType {
    HALL("hall"),
    DOME("dome"),
    STADIUM("stadium"),
    OTHER("other");

    @EnumeratedValue
    private final String value;

    VenueType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
