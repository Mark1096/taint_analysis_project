package taintanalysis.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import taintanalysis.error.ErrorException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static taintanalysis.error.ErrorCode.*;

/**
 * <h1> FileUtils </h1>
 *
 * This class is used to manage actions directed at files on the system and those provided as input by the user.
 */
public class FileUtils {

    public static final String CONFIG_FILE_PATH = "src/main/resources/config/config.json";
    public static final String SOURCE_BASE_PATH = "data/source/";
    public static final String DESTINATION_BASE_PATH = "data/destination/";

    /**
     * Returns the configured instance of JavaParser.
     *
     * @return java parser
     */
    private static JavaParser parserSetting() {
        var combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(Paths.get("src/main/java")));

        var parserConfiguration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));

        return new JavaParser(parserConfiguration);
    }

    /**
     * Returns an instance of CompilationUnit.
     *
     * @param sourceFilePath the source file path
     * @return compilation unit
     */
    public static CompilationUnit retrieveCompilationUnit(String sourceFilePath) {
        JavaParser javaParser = parserSetting();
        return javaParser.parse(getFileInputStream(sourceFilePath))
                .getResult()
                .orElseThrow();
    }

    /**
     * Returns a json object that contains information about the configuration file.
     *
     * @return json node
     */
    public static JsonNode getConfigInformation() {
        try {
            return new ObjectMapper().readTree(new File(CONFIG_FILE_PATH));
        } catch (IOException e) {
            throw generateRuntimeException(e);
        }
    }

    /**
     * Returns the input stream of the file based on the source provided as input.
     *
     * @param source the source
     * @return file input stream
     */
    private static FileInputStream getFileInputStream(String source) {
        try {
            return new FileInputStream(source);
        } catch (FileNotFoundException e) {
            throw generateRuntimeException(e);
        }
    }

    /**
     * Returns the list of java file names in the source directory that are to be parsed.
     *
     * @return list string
     * @throws ErrorException the error exception
     */
    public static List<String> getSourcesList() throws ErrorException {
        File directory = new File(SOURCE_BASE_PATH);

        if (!directory.exists() || !directory.isDirectory()) {
            throw generateErrorException(DIRECTORY_NOT_FOUND);
        }

        File[] files = directory.listFiles();
        if (files == null) {
            throw generateErrorException(FILE_NOT_FOUND);
        }

        return Arrays.stream(files)
                .filter(File::isFile)
                .map(File::getName)
                .filter(name -> name.endsWith(".java"))
                .collect(Collectors.toList());
    }

    /**
     * Inserts a new java file into the destination directory,
     * which contains the changes made by the program to the file passed as input by the user through the source directory.
     *
     * @param sourceFilePath the source file path
     * @param fileContent the file content
     */
    public static void writeOutputFile(String sourceFilePath, String fileContent) {
        String fileName = Paths.get(sourceFilePath).getFileName().toString();
        Path destinationDir = Paths.get(DESTINATION_BASE_PATH);
        Path destinationFile = destinationDir.resolve(fileName);
        try {
            if (Files.notExists(destinationDir)) {
                Files.createDirectories(destinationDir);
            }
            Files.write(destinationFile, fileContent.getBytes());
        } catch (Exception e) {
            throw generateException(e);
        }
    }

}
