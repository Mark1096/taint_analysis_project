package org.example.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ConfigClass {
    private String className;
    private List<String> methods;
    private List<ConstructorInfo> constructors;
}

