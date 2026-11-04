package se.arbetsprov.core.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

/**
 * Transport built on the JDK's {@link java.net.http.HttpClient}, used with its blocking
 * {@code send}. On a virtual thread the blocking call parks the thread instead of holding a
 * platform thread.
 */
public final class JdkHttpClient implements HttpClient {

    private final java.net.http.HttpClient client;

    /** A transport with a 10-second connect timeout and redirects followed. */
    public JdkHttpClient() {
        this(java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build());
    }

    /** A transport over a caller-configured JDK client. */
    public JdkHttpClient(java.net.http.HttpClient client) {
        this.client = client;
    }

    @Override
    public HttpResponse send(HttpRequest request) {
        var builder = java.net.http.HttpRequest.newBuilder(request.uri());
        byte[] body = request.body();
        builder.method(request.method().name(),
                body.length == 0 ? BodyPublishers.noBody() : BodyPublishers.ofByteArray(body));
        request.headers().asMap().forEach((name, values) -> values.forEach(v -> builder.header(name, v)));

        try {
            var response = client.send(builder.build(), BodyHandlers.ofByteArray());
            return new HttpResponse(request, response.statusCode(),
                    HttpHeaders.of(response.headers().map()), response.body());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedIOException(new InterruptedIOException("Interrupted while sending " + request));
        }
    }
}
