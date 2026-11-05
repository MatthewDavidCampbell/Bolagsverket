package se.arbetsprov.ai.catalog;

/** A suggested parameter value is outside the operation's schema; no request is built. */
public class InvalidParametersException extends RuntimeException {

    public InvalidParametersException(String message) {
        super(message);
    }
}
