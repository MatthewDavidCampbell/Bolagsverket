package se.arbetsprov.core.credential;

/**
 * An API key for a remote service. The key is kept out of {@link #toString()} so it cannot leak
 * into logs.
 *
 * @param key the key; never blank
 */
public record KeyCredential(String key) {

    public KeyCredential {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
    }

    /** A credential from {@code value}, or null when the value is null or blank. */
    public static KeyCredential ofNullable(String value) {
        return value == null || value.isBlank() ? null : new KeyCredential(value);
    }

    @Override
    public String toString() {
        return "KeyCredential[****]";
    }
}
