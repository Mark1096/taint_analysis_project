package taintanalysis.error;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * <h1> ErrorCode </h1>
 *
 * It is used to handle any exceptions that may occur during program execution.
 */
public enum ErrorCode {

    /**
     * Java file not found.
     */
    JAVA_FILE_NOT_FOUND("Java file not found"),

    /**
     * File not found.
     */
    FILE_NOT_FOUND("File not found"),

    /**
     * Directory not found.
     */
    DIRECTORY_NOT_FOUND("Directory not found");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    /**
     * Returns the error message.
     *
     * @return string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Generate a new exception with the appropriate error message.
     *
     * @param error the error code
     * @return error exception
     */
    public static ErrorException generateErrorException(ErrorCode error) {
        return new ErrorException(error.getMessage());
    }

    /**
     * Generate a runtime exception when the file is not found.
     *
     * @param error the error code
     * @return runtime exception
     */
    public static RuntimeException generateRuntimeException(FileNotFoundException error) {
        return new RuntimeException(error.getMessage());
    }

    /**
     * Generate a runtime exception when the I/O is not found.
     *
     * @param error the error code
     * @return IO exception
     */
    public static RuntimeException generateRuntimeException(IOException error) {
        return new RuntimeException(error.getMessage());
    }

    /**
     * Generate a runtime exception with the appropriate error message.
     *
     * @param error the error code
     * @return IO exception
     */
    public static RuntimeException generateException(Exception error) {
        return new RuntimeException(error.getMessage());
    }

}
