package software.cheeselooker.exceptions;

public class QueryEngineException extends Exception {
    public QueryEngineException(String message) {
        super(message);
    }

    public QueryEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
