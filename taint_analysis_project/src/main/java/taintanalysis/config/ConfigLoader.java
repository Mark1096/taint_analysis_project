package taintanalysis.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import org.apache.commons.collections4.CollectionUtils;
import taintanalysis.service.ConstructorAnalyzer;
import taintanalysis.utils.FileUtils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static taintanalysis.error.ErrorCode.generateRuntimeException;
import static taintanalysis.utils.FileUtils.CONFIG_FILE_PATH;

/**
 * <h1> ConfigLoader </h1>
 *
 * This class is used to load information from the configuration file and return it based on checks that are made.
 */
public class ConfigLoader {
    private final Map<String, Source> sources = new HashMap<>();
    public static ConfigLoader configLoader = new ConfigLoader();

    private ConfigLoader() {
        Gson gson = new Gson();
        try {
            var config = gson.fromJson(new FileReader(CONFIG_FILE_PATH), Config.class);
            insertSources(config);
        } catch (FileNotFoundException e) {
            throw generateRuntimeException(e);
        }
    }

    /**
     * Returns the only instance of the class.
     *
     * @return config loader
     */
    public static ConfigLoader getInstance() {
        return configLoader;
    }

    /**
     * It fills the list of sources with those found in the configuration file,
     * creating a hash map that associates each source with its classes.
     *
     * @param config the config
     */
    private void insertSources(Config config) {
        if (config != null && CollectionUtils.isNotEmpty(config.sources)) {
            for (Source source : config.sources) {
                for (ConfigClass configClass : source.getClasses()) {
                    sources.put(configClass.getClassName(), source);
                }
            }
        }
    }

    /**
     * Method to obtain untrusted source names from configuration file.
     *
     * @return list string
     */
    public List<String> getUntrustedSources() {
        JsonNode rootNode = FileUtils.getConfigInformation();

        return rootNode.path("sources").findValuesAsText("name").stream()
                .filter(source -> !rootNode.path("sources").findValue("trusted").asBoolean())
                .toList();
    }

    /**
     * Returns an instance of the source of the configuration file,
     * whose information matches that of the external source found in the user file.
     *
     * @param className the class name
     * @param currentMethod the current method
     * @param parameterContext the parameter context
     * @param staticMethod the static method
     * @return source
     */
    public Source getSourceDetailsForResolvedType(String className, String currentMethod,
                                                  List<String> parameterContext, boolean staticMethod) {
        return sources.values().stream()
                .flatMap(source -> source.getClasses().stream()
                        .filter(configClass -> matchesClassAndMethod(configClass, className, currentMethod))
                        .filter(configClass -> !staticMethod) // escludi subito i metodi statici
                        .filter(configClass -> hasNoConstructors(configClass) || hasMatchingConstructor(configClass, parameterContext))
                        .map(configClass -> source))
                .findFirst()
                .orElse(null);
    }

    /**
     * Verify that the class and method are the same.
     *
     * @param configClass the config class
     * @param className the class name
     * @param currentMethod the current method
     * @return boolean
     */
    private boolean matchesClassAndMethod(ConfigClass configClass, String className, String currentMethod) {
        return configClass.getClassName().equals(className) &&
                configClass.getMethods().contains(currentMethod);
    }

    /**
     * Verify that the constructor does not receive parameters.
     *
     * @param configClass the config class
     * @return boolean
     */
    private boolean hasNoConstructors(ConfigClass configClass) {
        return configClass.getConstructors().isEmpty();
    }

    /**
     * Compare constructor's parameters.
     *
     * @param configClass the config class
     * @param parameterContext the parameter context
     * @return boolean
     */
    private boolean hasMatchingConstructor(ConfigClass configClass, List<String> parameterContext) {
        return configClass.getConstructors().stream()
                .anyMatch(constructor -> ConstructorAnalyzer.getInstance().matchesConstructor(constructor, parameterContext));
    }

}
