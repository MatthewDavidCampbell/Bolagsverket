package se.arbetsprov.core.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.core.exception.DeserializationException;
import se.arbetsprov.core.exception.HttpResponseException;
import se.arbetsprov.core.http.policy.HttpPipelinePolicy;

/**
 * User story 1: call a remote service and get a typed result.
 * Runs over a real socket against a local WireMock server; no other network access.
 */
@DisplayName("Typed responses")
class TypedResponseTests {

    record Item(String name, long population) {
    }

    private final WireMockServer server = new WireMockServer(options().dynamicPort());
    private final HttpPipeline pipeline = HttpPipeline.builder().httpClient(new JdkHttpClient()).build();

    @BeforeEach
    void start() {
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    private HttpRequest getItems() {
        return HttpRequest.get(URI.create(server.baseUrl() + "/items"));
    }

    @Test
    void getList_whenServiceReturns200WithJsonArray_returnsTypedListWithSameItems() {
        // Given a pipeline with no policies and a stubbed service returning 200 with a JSON array of two items
        server.stubFor(get("/items").willReturn(okJson("""
                [{"name":"Sweden","population":10500000,"extra":true},
                 {"name":"Norway","population":5500000}]""")));

        // When the caller sends a GET and asks for a list of a test record type
        var response = pipeline.sendList(getItems(), Item.class);

        // Then the response status is 200 and the list holds two items with the same field values
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.value()).containsExactly(new Item("Sweden", 10_500_000), new Item("Norway", 5_500_000));
    }

    @Test
    void get_whenServiceReturns404_throwsHttpResponseExceptionWithStatusAndBody() {
        // Given a stubbed service returning 404 with a short text body
        server.stubFor(get("/items").willReturn(aResponse().withStatus(404).withBody("not here")));

        // When the caller sends a GET and asks for a typed result
        // Then an HttpResponseException is thrown carrying status 404 and that body
        assertThatThrownBy(() -> pipeline.send(getItems(), Item.class))
                .isInstanceOfSatisfying(HttpResponseException.class, e -> {
                    assertThat(e.status()).isEqualTo(404);
                    assertThat(e.body()).isEqualTo("not here");
                });
    }

    @Test
    void get_whenBodyIsNotValidJsonForType_throwsDeserializationExceptionNamingType() {
        // Given a stubbed service returning 200 with a body that does not match the requested type
        server.stubFor(get("/items").willReturn(okJson("{\"population\":\"many\"}")));

        // When the caller asks for a typed result
        // Then a DeserializationException is thrown whose message names the target type
        assertThatThrownBy(() -> pipeline.send(getItems(), Item.class))
                .isInstanceOf(DeserializationException.class)
                .hasMessageContaining(Item.class.getName());
    }

    @Test
    void getList_whenServiceReturns200WithEmptyBody_returnsEmptyList() {
        // Given a stubbed service returning 200 with an empty body
        server.stubFor(get("/items").willReturn(ok()));

        // When the caller asks for a list of a type
        var response = pipeline.sendList(getItems(), Item.class);

        // Then the response value is an empty list, not an error
        assertThat(response.value()).isEmpty();
    }

    @Test
    void get_whenServiceReturns200WithEmptyBody_returnsNullValue() {
        // Given a stubbed service returning 200 with an empty body
        server.stubFor(get("/items").willReturn(ok()));

        // When the caller asks for a single typed value
        var response = pipeline.send(getItems(), Item.class);

        // Then the response value is null and the status is 200
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.value()).isNull();
    }

    @Test
    void send_withPolicyAddedHeader_headerReachesTheService() {
        // Given a pipeline with a policy that adds a header and the real JDK transport
        server.stubFor(get("/items").willReturn(ok()));
        HttpPipelinePolicy addHeader = (request, next) -> next.process(request.withHeader("X-Test", "1"));
        var withPolicy = HttpPipeline.builder().policies(addHeader).httpClient(new JdkHttpClient()).build();

        // When a GET is sent
        withPolicy.send(getItems());

        // Then WireMock records the request with that header
        server.verify(getRequestedFor(urlEqualTo("/items")).withHeader("X-Test", equalTo("1")));
    }
}
