package se.arbetsprov.ai.service;

import java.io.UncheckedIOException;
import java.net.URI;
import se.arbetsprov.ai.AskException;
import se.arbetsprov.ai.catalog.RestCall;
import se.arbetsprov.core.client.ClientOptions;
import se.arbetsprov.core.http.HttpHeaders;
import se.arbetsprov.core.http.HttpMethod;
import se.arbetsprov.core.http.HttpPipeline;
import se.arbetsprov.core.http.HttpRequest;

/**
 * Sends catalog-built requests to the countries and visits service through the core pipeline.
 * Any status comes back as an answer; only a service that does not answer at all fails.
 */
public final class ServiceClient {

    /**
     * @param status HTTP status
     * @param body   response body as text (empty for 204)
     */
    public record ServiceResponse(int status, String body) {

        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }
    }

    private final URI baseUri;
    private final HttpPipeline pipeline;

    public ServiceClient(URI baseUri) {
        this.baseUri = baseUri;
        this.pipeline = new ClientOptions().baseUri(baseUri).buildPipeline();
    }

    public ServiceResponse send(RestCall call) {
        String base = baseUri.toString().replaceAll("/+$", "");
        var headers = HttpHeaders.empty();
        for (var h : call.headers().entrySet()) {
            headers = headers.with(h.getKey(), h.getValue());
        }
        if (call.body() != null) {
            headers = headers.with("Content-Type", "application/json");
        }
        var request = new HttpRequest(HttpMethod.valueOf(call.method()), URI.create(base + call.target()), headers, call.body());
        try {
            var response = pipeline.send(request);
            return new ServiceResponse(response.status(), response.bodyAsString());
        } catch (UncheckedIOException e) {
            throw new AskException(AskException.UNREACHABLE,
                    "Tjänsten svarar inte på " + baseUri + ". Starta den, t.ex. med Docker, och försök igen.", e);
        }
    }
}
