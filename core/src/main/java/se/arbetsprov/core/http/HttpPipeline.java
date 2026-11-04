package se.arbetsprov.core.http;

import java.util.List;
import java.util.Objects;
import se.arbetsprov.core.exception.HttpResponseException;
import se.arbetsprov.core.http.policy.HttpPipelinePolicy;
import se.arbetsprov.core.serialization.JsonSerializer;

/**
 * Sends requests through ordered policies down to a transport. The request passes the policies
 * in the order given, the response passes them in reverse order on the way back.
 *
 * <p>Modelled on Azure Core's {@code HttpPipeline}, but synchronous: callers run on virtual
 * threads, so there are no reactive types. The pipeline is immutable and holds no per-call
 * state, so one instance can be shared by any number of concurrent callers.
 */
public final class HttpPipeline {

    private final List<HttpPipelinePolicy> policies;
    private final HttpClient httpClient;
    private final JsonSerializer serializer = new JsonSerializer();

    private HttpPipeline(List<HttpPipelinePolicy> policies, HttpClient httpClient) {
        this.policies = List.copyOf(policies);
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    /** A builder with no policies and the JDK transport. */
    public static Builder builder() {
        return new Builder();
    }

    /** The policies, in request order. */
    public List<HttpPipelinePolicy> policies() {
        return policies;
    }

    /** Sends the request and returns the response whatever its status. */
    public HttpResponse send(HttpRequest request) {
        return next(0).process(request);
    }

    /**
     * Sends the request and turns the body into {@code type}.
     *
     * @throws HttpResponseException when the status is not 2xx
     * @throws se.arbetsprov.core.exception.DeserializationException when the body does not fit
     */
    public <T> Response<T> send(HttpRequest request, Class<T> type) {
        HttpResponse response = sendExpectingSuccess(request);
        return new Response<>(response.status(), response.headers(),
                serializer.deserialize(response.body(), type));
    }

    /**
     * Sends the request and turns a JSON array body into a list of {@code type}.
     *
     * @throws HttpResponseException when the status is not 2xx
     * @throws se.arbetsprov.core.exception.DeserializationException when the body does not fit
     */
    public <T> Response<List<T>> sendList(HttpRequest request, Class<T> type) {
        HttpResponse response = sendExpectingSuccess(request);
        return new Response<>(response.status(), response.headers(),
                serializer.deserializeList(response.body(), type));
    }

    private HttpResponse sendExpectingSuccess(HttpRequest request) {
        HttpResponse response = send(request);
        if (!response.isSuccess()) {
            throw new HttpResponseException(response.status(), response.bodyAsString());
        }
        return response;
    }

    // The chain is built per call from the immutable list, so no state is shared between calls.
    private HttpPipelineNextPolicy next(int index) {
        if (index == policies.size()) {
            return httpClient::send;
        }
        return request -> policies.get(index).process(request, next(index + 1));
    }

    /** Builds an {@link HttpPipeline}. */
    public static final class Builder {

        private List<HttpPipelinePolicy> policies = List.of();
        private HttpClient httpClient;

        private Builder() {
        }

        /** The policies, in request order. Replaces any set before. */
        public Builder policies(HttpPipelinePolicy... policies) {
            return policies(List.of(policies));
        }

        /** The policies, in request order. Replaces any set before. */
        public Builder policies(List<HttpPipelinePolicy> policies) {
            this.policies = List.copyOf(policies);
            return this;
        }

        /** The transport; defaults to {@link JdkHttpClient}. */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public HttpPipeline build() {
            return new HttpPipeline(policies, httpClient == null ? new JdkHttpClient() : httpClient);
        }
    }
}
