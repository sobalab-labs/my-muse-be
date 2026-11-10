package com.sobalab.mymuse.api.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.EnumeratedValue;

public enum SupportedLocale {
    KO("ko"),
    JA("ja");

    @EnumeratedValue
    private final String value;

    SupportedLocale(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
