package com.sobalab.mymuse.api.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.EnumeratedValue;

public enum CountryCode {
    JP("JP"),
    KR("KR");

    @EnumeratedValue
    private final String value;

    CountryCode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
