package se.arbetsprov.core.exception;

/**
 * Thrown by typed sends when the remote service answers with a non-success status.
 */
public class HttpResponseException extends RuntimeException {

    private final int status;
    private final String body;

    public HttpResponseException(int status, String body) {
        super("Remote service returned status " + status);
        this.status = status;
        this.body = body;
    }

    /** The HTTP status code. */
    public int status() {
        return status;
    }

    /** The response body as text; empty when there was none. */
    public String body() {
        return body;
    }
}
