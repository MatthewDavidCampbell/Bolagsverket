package se.arbetsprov.core.client;

import java.net.URI;
import java.util.List;
import se.arbetsprov.core.http.HttpClient;
import se.arbetsprov.core.http.HttpPipeline;
import se.arbetsprov.core.http.JdkHttpClient;
import se.arbetsprov.core.http.policy.AuthorizationPolicy;
import se.arbetsprov.core.http.policy.HttpPipelinePolicy;
import se.arbetsprov.core.http.policy.RetryPolicy;
import se.arbetsprov.core.http.policy.RetryStrategy;

/**
 * Settings a service client is built from. Service libraries extend this class with their own
 * settings; the application maps its configuration onto it.
 *
 * <p>Defaults: the JDK transport, and the policies retry then authorization.
 */
public class ClientOptions {

    private URI baseUri;
    private HttpClient httpClient;
    private List<HttpPipelinePolicy> policies;

    /** The address that request paths are resolved against. Required. */
    public ClientOptions baseUri(URI baseUri) {
        this.baseUri = baseUri;
        return this;
    }

    /** The transport; defaults to {@link JdkHttpClient}. */
    public ClientOptions httpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    /** The policies, in request order; replaces the defaults. */
    public ClientOptions policies(HttpPipelinePolicy... policies) {
        this.policies = List.of(policies);
        return this;
    }

    /** The configured base address, or null. */
    public URI baseUri() {
        return baseUri;
    }

    /** Resolves a relative path, including any query, against the base address. */
    public URI resolve(String path) {
        return requireBaseUri().resolve(path);
    }

    /**
     * Builds the pipeline from these options.
     *
     * @throws IllegalStateException when {@code baseUri} is not set
     */
    public HttpPipeline buildPipeline() {
        requireBaseUri();
        return HttpPipeline.builder()
                .policies(policies != null ? policies : defaultPolicies())
                .httpClient(httpClient != null ? httpClient : new JdkHttpClient())
                .build();
    }

    private static List<HttpPipelinePolicy> defaultPolicies() {
        // Authorization sits inside retry so that each attempt would get a fresh credential.
        return List.of(new RetryPolicy(RetryStrategy.EXPONENTIAL_BACKOFF), new AuthorizationPolicy());
    }

    private URI requireBaseUri() {
        if (baseUri == null) {
            throw new IllegalStateException("baseUri is required but was not set");
        }
        return baseUri;
    }
}
