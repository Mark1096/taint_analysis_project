package org.example.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ConstructorInfo {
    private List<String> parameterTypes;
    private List<String> parameters;
}

