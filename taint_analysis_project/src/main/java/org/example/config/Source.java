package org.example.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Source {
    private String name;
    private boolean trusted;
    private List<ConfigClass> classes;
}

