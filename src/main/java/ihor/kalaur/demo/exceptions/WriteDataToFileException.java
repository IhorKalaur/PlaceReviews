package ihor.kalaur.demo.exceptions;

public class WriteDataToFileException extends RuntimeException {
    public WriteDataToFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
