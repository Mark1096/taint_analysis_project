package org.example;

import org.example.config.ConfigLoader;
import org.example.service.Analyzer;

public class Main {
    public static void main(String[] args) {

        try {
            String configFilePath = "src/main/resources/config/config.json";
            // Caricamento del file di configurazione
            ConfigLoader loader = new ConfigLoader(configFilePath);

            // Creazione dell'analizzatore con il loader di configurazione e gli argomenti della riga di comando
            Analyzer analyzer = new Analyzer(loader, args, configFilePath);

            // Analisi del file sorgente specificato
            String sourceFilePath = "src/main/java/org/example/source/ClassToAnalyze2.java";
            analyzer.analyze(sourceFilePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
