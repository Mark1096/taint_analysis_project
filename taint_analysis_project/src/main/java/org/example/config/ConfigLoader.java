package org.example.config;

import com.google.gson.Gson;
import org.example.service.ConstructorAnalyzer;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class ConfigLoader {
    private final Map<String, Source> sources = new HashMap<>();

    public ConfigLoader(String configFilePath) throws IOException {
        Gson gson = new Gson();
        Config config = gson.fromJson(new FileReader(configFilePath), Config.class);
        for (Source source : config.sources) {
            for (ConfigClass configClass : source.getClasses()) {
                sources.put(configClass.getClassName(), source);
            }
        }
    }

    public boolean isSourceTrusted(String sourceName) {
        return sources.values().stream()
                .filter(source -> source.getName().equals(sourceName))
                .map(Source::isTrusted)
                .findFirst()
                .orElse(true);
    }

    public Source getSourceDetailsForResolvedType(String className, String currentMethod,
                                                     List<String> parameterContext, boolean staticMethod) {
        return sources.values().stream()
                .flatMap(source -> source.getClasses().stream()
                        .filter(configClass -> configClass.getClassName().equals(className))
                        .filter(configClass -> configClass.getMethods().contains(currentMethod))
                        .flatMap(configClass -> {
                            if (staticMethod || configClass.getConstructors().isEmpty()) {
                                return Stream.empty();  // Probabilmente si potrebbe restituire direttamente null, piuttosto che un oggetto. Da valutare!
                            }
                            return configClass.getConstructors().stream()
                                .anyMatch(constructor -> ConstructorAnalyzer.matchesConstructor(constructor, parameterContext))
                                ? Stream.of(source)
                                : Stream.empty();
                        }))
                .findFirst()
                .orElse(null);
    }

    private static class Config {
        public List<Source> sources;
    }

}