package se.arbetsprov.core.http;

/**
 * The transport at the bottom of the pipeline: sends one request over the network and blocks
 * until the response arrives. Blocking is cheap on virtual threads, so there is no async variant.
 * Tests replace it with a fake.
 */
@FunctionalInterface
public interface HttpClient {

    /**
     * Sends the request.
     *
     * @throws java.io.UncheckedIOException when the request cannot be sent or the response read
     */
    HttpResponse send(HttpRequest request);
}
