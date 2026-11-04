package se.arbetsprov.core.http;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * An immutable response from a remote service, as seen by policies on the way back up.
 *
 * @param request the request that produced this response
 * @param status  the HTTP status code
 * @param headers the response headers
 * @param body    the body bytes; empty when there is no body
 */
public record HttpResponse(HttpRequest request, int status, HttpHeaders headers, byte[] body) {

    public HttpResponse {
        Objects.requireNonNull(request, "request");
        headers = headers == null ? HttpHeaders.empty() : headers;
        body = body == null ? new byte[0] : body.clone();
    }

    /** True for 2xx status codes. */
    public boolean isSuccess() {
        return status >= 200 && status < 300;
    }

    /** The body decoded as UTF-8. */
    public String bodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof HttpResponse response
                && status == response.status
                && request.equals(response.request)
                && headers.equals(response.headers)
                && Arrays.equals(body, response.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(request, status, headers, Arrays.hashCode(body));
    }

    @Override
    public String toString() {
        return status + " for " + request;
    }
}
