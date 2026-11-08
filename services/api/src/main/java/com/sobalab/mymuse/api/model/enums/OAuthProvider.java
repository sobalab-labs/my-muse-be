package com.sobalab.mymuse.api.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.EnumeratedValue;

public enum OAuthProvider {
    GOOGLE("google"),
    KAKAO("kakao");

    @EnumeratedValue
    private final String value;

    OAuthProvider(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
