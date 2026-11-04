package se.arbetsprov.core.exception;

/**
 * Thrown when a response body cannot be turned into the requested type.
 */
public class DeserializationException extends RuntimeException {

    private final Class<?> targetType;

    public DeserializationException(Class<?> targetType, Throwable cause) {
        super("Could not deserialize response body to " + targetType.getName(), cause);
        this.targetType = targetType;
    }

    /** The type the body was meant to become. */
    public Class<?> targetType() {
        return targetType;
    }
}
