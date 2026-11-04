package se.arbetsprov.core.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.core.http.HttpRequest;
import se.arbetsprov.core.http.policy.AuthorizationPolicy;
import se.arbetsprov.core.http.policy.RetryPolicy;
import se.arbetsprov.core.testing.FakeHttpClient;
import se.arbetsprov.core.testing.RecordingPolicy;

/**
 * Base options that service libraries extend for their own clients.
 */
@DisplayName("Client options")
class ClientOptionsTests {

    @Test
    void buildPipeline_withoutBaseUri_failsNamingBaseUri() {
        // Given client options with no base URI
        var options = new ClientOptions();

        // When the pipeline is built
        // Then an IllegalStateException is thrown whose message names "baseUri"
        assertThatThrownBy(options::buildPipeline)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("baseUri");
    }

    @Test
    void buildPipeline_withDefaults_usesRetryThenAuthorization() {
        // Given client options with only a base URI
        var options = new ClientOptions().baseUri(URI.create("https://example.test/"));

        // When the pipeline is built
        var pipeline = options.buildPipeline();

        // Then its policies are RetryPolicy first and AuthorizationPolicy second
        // (the skeletons do nothing observable, so the order is read from the pipeline)
        assertThat(pipeline.policies())
                .hasExactlyElementsOfTypes(RetryPolicy.class, AuthorizationPolicy.class);
    }

    @Test
    void resolve_withRelativePath_joinsWithBaseUri() {
        // Given base URI "https://example.test/v3.1/"
        var options = new ClientOptions().baseUri(URI.create("https://example.test/v3.1/"));

        // When "all?fields=name" is resolved
        var uri = options.resolve("all?fields=name");

        // Then the result is "https://example.test/v3.1/all?fields=name"
        assertThat(uri).isEqualTo(URI.create("https://example.test/v3.1/all?fields=name"));
    }

    @Test
    void policies_whenReplaced_buildPipelineUsesOnlyThoseInGivenOrder() {
        // Given client options with two custom recording policies set explicitly
        var log = new ArrayList<String>();
        var options = new ClientOptions()
                .baseUri(URI.create("https://example.test/"))
                .httpClient(FakeHttpClient.logging(200, log))
                .policies(new RecordingPolicy("first", log), new RecordingPolicy("second", log));

        // When the pipeline is built and a request is sent
        options.buildPipeline().send(HttpRequest.get(options.resolve("items")));

        // Then only those two policies ran, in the order given
        assertThat(log).containsExactly(
                "first-request", "second-request", "transport", "second-response", "first-response");
    }
}
