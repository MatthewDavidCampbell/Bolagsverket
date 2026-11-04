package se.arbetsprov.core.http.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.core.http.HttpPipeline;
import se.arbetsprov.core.http.HttpRequest;
import se.arbetsprov.core.http.HttpResponse;
import se.arbetsprov.core.testing.FakeHttpClient;

/**
 * User story 3: retry and authorization are documented extension points that pass through.
 * Tested only for what they do today: keep order and leave the request unchanged.
 */
@DisplayName("Skeleton policies")
class SkeletonPolicyTests {

    private static final HttpRequest REQUEST = HttpRequest.get(URI.create("https://example.test/items"))
            .withHeader("Accept", "application/json");

    @Test
    void retryPolicy_whenTransportFailsOnce_callsTransportOnceAndPropagatesFailure() {
        // Given a pipeline with RetryPolicy(EXPONENTIAL_BACKOFF) and a transport that throws on its first call
        var calls = new AtomicInteger();
        var failure = new UncheckedIOException(new IOException("connection reset"));
        var pipeline = HttpPipeline.builder()
                .policies(new RetryPolicy(RetryStrategy.EXPONENTIAL_BACKOFF))
                .httpClient(request -> {
                    if (calls.incrementAndGet() == 1) {
                        throw failure;
                    }
                    return new HttpResponse(request, 200, null, null);
                })
                .build();

        // When a request is sent
        // Then the transport was called exactly once and the exception reaches the caller
        assertThatThrownBy(() -> pipeline.send(REQUEST)).isSameAs(failure);
        assertThat(calls).hasValue(1);
    }

    @Test
    void retryPolicy_passesRequestAndResponseThroughUnchanged() {
        // Given a pipeline with RetryPolicy(FIXED_DELAY) and a fake transport returning 200
        var expected = new HttpResponse(REQUEST, 200, null, "ok".getBytes());
        var transport = FakeHttpClient.answering(request -> expected);
        var pipeline = HttpPipeline.builder()
                .policies(new RetryPolicy(RetryStrategy.FIXED_DELAY))
                .httpClient(transport)
                .build();

        // When a request is sent
        var response = pipeline.send(REQUEST);

        // Then the transport receives an equal request and the caller receives the same response
        assertThat(transport.requests()).containsExactly(REQUEST);
        assertThat(response).isSameAs(expected);
    }

    @Test
    void retryStrategy_offersExponentialBackoffAndFixedDelay() {
        // Given the RetryStrategy enum
        // When its values are listed
        // Then they are exactly EXPONENTIAL_BACKOFF and FIXED_DELAY
        assertThat(RetryStrategy.values())
                .containsExactly(RetryStrategy.EXPONENTIAL_BACKOFF, RetryStrategy.FIXED_DELAY);
    }

    @Test
    void authorizationPolicy_leavesHeadersUnchanged() {
        // Given a pipeline with AuthorizationPolicy and a request with one header
        var transport = FakeHttpClient.returning(200);
        var pipeline = HttpPipeline.builder().policies(new AuthorizationPolicy()).httpClient(transport).build();

        // When the request is sent
        pipeline.send(REQUEST);

        // Then the transport receives exactly that one header, nothing added or removed
        assertThat(transport.requests()).singleElement()
                .satisfies(r -> assertThat(r.headers()).isEqualTo(REQUEST.headers()));
    }
}
