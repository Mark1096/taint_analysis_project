package taintanalysis.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import org.apache.commons.collections4.CollectionUtils;
import taintanalysis.service.ConstructorAnalyzer;
import taintanalysis.utils.FileUtils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static taintanalysis.utils.FileUtils.CONFIG_FILE_PATH;

public class ConfigLoader {
    private final Map<String, Source> sources = new HashMap<>();
    public static ConfigLoader configLoader = new ConfigLoader();;

    private ConfigLoader() {
        sourcesConfiguration();
    }

    public static ConfigLoader getInstance() {
        return configLoader;
    }

    // TODO: capire se è possibile catturare l'eccezione per reindirizzarla alla classe apposita.
    public void sourcesConfiguration() {
        Gson gson = new Gson();
        Config config;
        try {
            config = gson.fromJson(new FileReader(CONFIG_FILE_PATH), Config.class);
            insertSources(config);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    private void insertSources(Config config) {
        if (config != null && CollectionUtils.isNotEmpty(config.sources)) {
            for (Source source : config.sources) {
                for (ConfigClass configClass : source.getClasses()) {
                    sources.put(configClass.getClassName(), source);
                }
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

    // Metodo per ottenere i nomi delle sorgenti con trusted=false
    public List<String> getUntrustedSources() throws IOException {
        JsonNode rootNode = FileUtils.getConfigInformation();

        // Estrai le sorgenti non fidate
        return rootNode.path("sources").findValuesAsText("name").stream()
                .filter(source -> !rootNode.path("sources").findValue("trusted").asBoolean())
                .toList();
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
                                    .anyMatch(constructor -> ConstructorAnalyzer.getInstance().matchesConstructor(constructor, parameterContext))
                                    ? Stream.of(source)
                                    : Stream.empty();
                        }))
                .findFirst()
                .orElse(null);
    }

    // TODO: valutare la possibilità di spostare questa classe in un file a parte.
    private static class Config {
        public List<Source> sources;
    }

}