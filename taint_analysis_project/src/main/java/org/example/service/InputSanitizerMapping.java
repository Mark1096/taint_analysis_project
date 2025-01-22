package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InputSanitizerMapping {

    public static Map<String, String> creationMapping(String configFilePath) {
        // Percorso al file di configurazione JSON
     //   String configFilePath = "src/main/resources/config/config.json"; // Sostituisci con il percorso corretto
        // Mappatura: Nome della sorgente -> Metodo di sanitizzazione
        Map<String, String> sanitizationMethods = new HashMap<>();

        try {
            // Leggi il file di configurazione e ottieni i nomi delle sorgenti con trusted=false
            String[] inputSources = getUntrustedSources(configFilePath);

            // Creazione della mappatura
            for (String source : inputSources) {
                String methodName = "sanitize" + capitalizeFirstLetter(source);
                sanitizationMethods.put(source, "InputSanitizer." + methodName);
            }

        } catch (IOException e) {
            System.err.println("Errore durante la lettura del file di configurazione: " + e.getMessage());
        }
        return sanitizationMethods;
    }

    // Metodo per ottenere i nomi delle sorgenti con trusted=false
    private static String[] getUntrustedSources(String configFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        // Leggi il file JSON
        JsonNode rootNode = objectMapper.readTree(new File(configFilePath));

        // Estrai le sorgenti non fidate
        return rootNode.path("sources").findValuesAsText("name").stream()
                .filter(source -> !rootNode.path("sources").findValue("trusted").asBoolean())
                .toArray(String[]::new);
    }

    // Metodo per capitalizzare la prima lettera di una stringa
    private static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
