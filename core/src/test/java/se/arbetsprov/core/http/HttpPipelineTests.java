package se.arbetsprov.core.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.core.http.policy.HttpPipelinePolicy;
import se.arbetsprov.core.testing.FakeHttpClient;
import se.arbetsprov.core.testing.RecordingPolicy;

/**
 * User story 2: compose ordered policies around every call.
 * Uses a fake transport and recording policies; no network.
 */
@DisplayName("HTTP pipeline")
class HttpPipelineTests {

    private static final HttpRequest REQUEST = HttpRequest.get(URI.create("https://example.test/items"));

    @Test
    void send_withPoliciesAThenB_runsRequestInOrderAndResponseInReverse() {
        // Given recording policies A then B and a fake transport
        var log = new ArrayList<String>();
        var pipeline = HttpPipeline.builder()
                .policies(new RecordingPolicy("A", log), new RecordingPolicy("B", log))
                .httpClient(FakeHttpClient.logging(200, log))
                .build();

        // When one request is sent
        pipeline.send(REQUEST);

        // Then the recorded order is A-request, B-request, transport, B-response, A-response
        assertThat(log).containsExactly("A-request", "B-request", "transport", "B-response", "A-response");
    }

    @Test
    void send_withPolicyAddingHeader_transportSeesHeader() {
        // Given a policy that adds header "X-Test: 1" and a fake transport that keeps the request it received
        HttpPipelinePolicy addHeader = (request, next) -> next.process(request.withHeader("X-Test", "1"));
        var transport = FakeHttpClient.returning(200);
        var pipeline = HttpPipeline.builder().policies(addHeader).httpClient(transport).build();

        // When a request is sent
        pipeline.send(REQUEST);

        // Then the transport's request carries "X-Test: 1" and the caller's original request is unchanged
        assertThat(transport.requests()).singleElement()
                .satisfies(r -> assertThat(r.headers().first("x-test")).contains("1"));
        assertThat(REQUEST.headers().size()).isZero();
    }

    @Test
    void send_whenTransportThrows_sameExceptionReachesCaller() {
        // Given two pass-through policies and a transport that throws UncheckedIOException
        var failure = new UncheckedIOException(new IOException("connection refused"));
        var log = new ArrayList<String>();
        var pipeline = HttpPipeline.builder()
                .policies(new RecordingPolicy("A", log), new RecordingPolicy("B", log))
                .httpClient(request -> { throw failure; })
                .build();

        // When a request is sent
        // Then the very same exception instance reaches the caller
        assertThatThrownBy(() -> pipeline.send(REQUEST)).isSameAs(failure);
    }

    @Test
    void send_withNoPolicies_goesStraightToTransport() {
        // Given a pipeline with no policies and a fake transport returning 204
        var transport = FakeHttpClient.returning(204);
        var pipeline = HttpPipeline.builder().httpClient(transport).build();

        // When a request is sent
        var response = pipeline.send(REQUEST);

        // Then the transport is called once and the caller gets status 204
        assertThat(transport.requests()).hasSize(1);
        assertThat(response.status()).isEqualTo(204);
    }

    @Test
    void send_fromManyVirtualThreads_eachCallerGetsItsOwnResponse() throws Exception {
        // Given one pipeline whose fake transport echoes a request header back as a response header
        var transport = FakeHttpClient.answering(request -> new HttpResponse(request, 200,
                HttpHeaders.empty().with("X-Caller", request.headers().first("X-Caller").orElseThrow()), null));
        var log = new java.util.concurrent.CopyOnWriteArrayList<String>();
        var pipeline = HttpPipeline.builder().policies(new RecordingPolicy("A", log)).httpClient(transport).build();

        // When 1,000 virtual threads each send a request with their own header value
        List<Future<String>> results;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            results = IntStream.range(0, 1_000)
                    .mapToObj(String::valueOf)
                    .map(id -> executor.submit(() ->
                            pipeline.send(REQUEST.withHeader("X-Caller", id)).headers().first("X-Caller").orElseThrow()))
                    .toList();
        }

        // Then every caller receives the response for its own request
        for (int i = 0; i < results.size(); i++) {
            assertThat(results.get(i).get()).isEqualTo(String.valueOf(i));
        }
    }
}
