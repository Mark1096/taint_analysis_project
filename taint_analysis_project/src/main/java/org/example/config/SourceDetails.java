package org.example.config;

import lombok.Getter;

@Getter
public class SourceDetails {
    private final String name;
    private final boolean trusted;

    public SourceDetails(String name, boolean trusted) {
        this.name = name;
        this.trusted = trusted;
    }
}
