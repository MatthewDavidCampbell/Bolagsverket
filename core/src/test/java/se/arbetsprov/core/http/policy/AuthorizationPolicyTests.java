package se.arbetsprov.core.http.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.core.credential.KeyCredential;
import se.arbetsprov.core.http.HttpPipeline;
import se.arbetsprov.core.http.HttpRequest;
import se.arbetsprov.core.testing.FakeHttpClient;

/**
 * The authorization policy sends an API key as a bearer token, or nothing when there is no key.
 */
@DisplayName("Authorization policy")
class AuthorizationPolicyTests {

    private static final HttpRequest REQUEST = HttpRequest.get(URI.create("https://example.test/items"))
            .withHeader("Accept", "application/json");

    @Test
    void process_withCredential_addsBearerHeader() {
        // Given a pipeline with AuthorizationPolicy holding key "test-key"
        var transport = FakeHttpClient.returning(200);
        var pipeline = HttpPipeline.builder()
                .policies(new AuthorizationPolicy(new KeyCredential("test-key")))
                .httpClient(transport)
                .build();

        // When a request is sent
        pipeline.send(REQUEST);

        // Then the transport receives "Authorization: Bearer test-key" next to the original header
        assertThat(transport.requests()).singleElement().satisfies(r -> {
            assertThat(r.headers().first("Authorization")).contains("Bearer test-key");
            assertThat(r.headers().first("Accept")).contains("application/json");
        });
    }

    @Test
    void process_withoutCredential_leavesHeadersUnchanged() {
        // Given a pipeline with AuthorizationPolicy and no credential
        var transport = FakeHttpClient.returning(200);
        var pipeline = HttpPipeline.builder().policies(new AuthorizationPolicy()).httpClient(transport).build();

        // When a request with one header is sent
        pipeline.send(REQUEST);

        // Then the transport receives exactly that one header, nothing added or removed
        assertThat(transport.requests()).singleElement()
                .satisfies(r -> assertThat(r.headers()).isEqualTo(REQUEST.headers()));
    }

    @Test
    void keyCredential_toString_doesNotRevealKey() {
        // Given a credential holding "secret-key"
        var credential = new KeyCredential("secret-key");

        // When it is turned into text, for example by a logger
        // Then the key does not appear
        assertThat(credential.toString()).doesNotContain("secret-key");
    }

    @Test
    void keyCredential_whenBlank_isRejectedOrAbsent() {
        // Given a blank key
        // When a credential is created directly, or from an optional value
        // Then direct creation fails and the optional form gives no credential
        assertThatThrownBy(() -> new KeyCredential(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(KeyCredential.ofNullable(" ")).isNull();
        assertThat(KeyCredential.ofNullable(null)).isNull();
    }
}
