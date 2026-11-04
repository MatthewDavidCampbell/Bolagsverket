package se.arbetsprov.core.http;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

/**
 * An immutable request to a remote service. Policies change a request by creating a new one,
 * for example with {@link #withHeader(String, String)}.
 *
 * @param method the HTTP method
 * @param uri    the absolute address
 * @param headers the request headers
 * @param body   the body bytes; empty when there is no body
 */
public record HttpRequest(HttpMethod method, URI uri, HttpHeaders headers, byte[] body) {

    public HttpRequest {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(uri, "uri");
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("uri must be absolute: " + uri);
        }
        headers = headers == null ? HttpHeaders.empty() : headers;
        body = body == null ? new byte[0] : body.clone();
    }

    /** A GET request with no headers. */
    public static HttpRequest get(URI uri) {
        return new HttpRequest(HttpMethod.GET, uri, HttpHeaders.empty(), null);
    }

    /** A copy with one more header value. */
    public HttpRequest withHeader(String name, String value) {
        return new HttpRequest(method, uri, headers.with(name, value), body);
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof HttpRequest request
                && method == request.method
                && uri.equals(request.uri)
                && headers.equals(request.headers)
                && Arrays.equals(body, request.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, uri, headers, Arrays.hashCode(body));
    }

    @Override
    public String toString() {
        return method + " " + uri;
    }
}
