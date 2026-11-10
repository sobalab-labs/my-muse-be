package com.sobalab.mymuse.api.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.EnumeratedValue;

public enum LinkPlatform {
    INSTAGRAM("instagram"),
    YOUTUBE("youtube"),
    TIKTOK("tiktok"),
    TWITTER("twitter"),
    OFFICIAL("official");

    @EnumeratedValue
    private final String value;

    LinkPlatform(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
