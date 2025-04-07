package taintanalysis.error;

import lombok.Getter;

import java.io.FileNotFoundException;
import java.io.IOException;

@Getter
public enum ErrorCode {

    //BAD_WRITING_FILE("Error in writing file"),    // TODO: capire se e dove utilizzarlo.

    JAVA_FILE_NOT_FOUND("Java file not found"),

    FILE_NOT_FOUND("File not found"),

    DIRECTORY_NOT_FOUND("Directory not found");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public static ErrorException generateErrorException(ErrorCode error) {
        return new ErrorException(error.getMessage());
    }

    public static RuntimeException generateRuntimeException(FileNotFoundException error) {
        return new RuntimeException(error.getMessage());
    }

    public static RuntimeException generateRuntimeException(IOException error) {
        return new RuntimeException(error.getMessage());
    }

    public static RuntimeException generateException(Exception error) {
        return new RuntimeException(error.getMessage());
    }

}
