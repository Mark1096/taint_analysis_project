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

    public SourceDetails getSourceDetailsForResolvedType(String className, String currentMethod,
                                                         List<String> parameterContext, boolean staticMethod) {
        for (Source source : sources.values()) {
            for (ConfigClass configClass : source.getClasses()) {
                if (configClass.getClassName().equals(className)) {
                    if(configClass.getMethods().contains(currentMethod)) {
                        if(staticMethod || configClass.getConstructors().isEmpty()) {
                            return new SourceDetails(source.getName(), source.isTrusted());
                        }
                        else {
                            for (ConstructorInfo constructor : configClass.getConstructors()) {
                                if (matchesConstructor(constructor, parameterContext)) {
                                    return new SourceDetails(source.getName(), source.isTrusted());
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean matchesConstructor(ConstructorInfo constructor, List<String> constructorArgs) {

        if (constructor.getParameterTypes().size() != constructorArgs.size()) {
            return false;
        }
        
        return constructor.getParameterTypes().equals(constructorArgs);
    }

    private static class Config {
        List<Source> sources;
    }
}

