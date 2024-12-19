package org.example.config;

import com.google.gson.Gson;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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
        for (Source source : sources.values()) {
            if (source.getName().equals(sourceName)) {
                return source.isTrusted();
            }
        }
        return true;
    }

    public SourceDetails getSourceDetailsForResolvedType(String className, List<String> parameterContext) {

        for (Source source : sources.values()) {
            for (ConfigClass configClass : source.getClasses()) {
                if (configClass.getClassName().equals(className)) {
                    System.out.println("Config class: " + configClass.getClassName());
                    System.out.println("Class Name: " + className);
                    for (ConstructorInfo constructor : configClass.getConstructors()) {
                        if (matchesConstructor(constructor, parameterContext)) {
                            return new SourceDetails(source.getName(), source.isTrusted());
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean matchesConstructor(ConstructorInfo constructor, List<String> constructorArgs) {

        System.out.println("constructor config: " + constructor.getParameterTypes());
        System.out.println("constructor args: " + constructorArgs);
        System.out.println("constructors config number: " + constructor.getParameterTypes().size());
        System.out.println("constructor args number: " + constructorArgs.size());

        if (constructor.getParameterTypes().size() != constructorArgs.size()) {
            return false;
        }

        System.out.println("Supero il controllo sulla dimensione!");

        return constructor.getParameterTypes().equals(constructorArgs);
    }

    private static class Config {
        List<Source> sources;
    }
}

