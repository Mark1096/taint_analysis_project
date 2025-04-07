package taintanalysis.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import taintanalysis.error.ErrorException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static taintanalysis.error.ErrorCode.*;

@NoArgsConstructor
public class FileUtils {

    public static final String CONFIG_FILE_PATH = "src/main/resources/config/config.json";
    public static final String SOURCE_BASE_PATH = "data/source/";
    public static final String DESTINATION_BASE_PATH = "data/destination/";

    public static JsonNode getConfigInformation() {
        try {
            return new ObjectMapper().readTree(new File(CONFIG_FILE_PATH));
        } catch (IOException e) {
            throw generateRuntimeException(e);
        }
    }

    public static FileInputStream getFileInputStream(String source) {
        try {
            return new FileInputStream(source);
        } catch (FileNotFoundException e) {
            throw generateRuntimeException(e);
        }
    }

    public static List<String> getSourcesList() throws ErrorException {
        File directory = new File(SOURCE_BASE_PATH);

        // Verifica se la directory esiste ed è effettivamente una cartella
        if (!directory.exists() || !directory.isDirectory()) {
           // throw new Exception("Directory non trovata o non valida: " + SOURCE_BASE_PATH);
            throw generateErrorException(DIRECTORY_NOT_FOUND);
        }

        // Ottiene la lista dei file presenti, verificando che non sia null
        File[] files = directory.listFiles();
        if (files == null) {
            throw generateErrorException(FILE_NOT_FOUND);
        }

        // Filtra solo i file .java
        return Arrays.stream(files)
                .filter(File::isFile) // Controlla che sia un file
                .map(File::getName)     // Estrae solo il nome del file
                .filter(name -> name.endsWith(".java")) // Filtra solo i file Java
                .collect(Collectors.toList());
    }

    public static void writeOutputFile(String sourceFilePath, String fileContent) {
        String fileName = Paths.get(sourceFilePath).getFileName().toString();
        try {
            Files.write(Paths.get(DESTINATION_BASE_PATH + fileName), fileContent.getBytes());
        } catch (Exception e) {
            throw generateException(e);
        }
    }

}
