package software.cheeselooker.exceptions;

public class IndexerException extends Exception {
    public IndexerException(String message) {
        super(message);
    }

    public IndexerException(String message, Throwable cause) {
        super(message, cause);
    }
}
