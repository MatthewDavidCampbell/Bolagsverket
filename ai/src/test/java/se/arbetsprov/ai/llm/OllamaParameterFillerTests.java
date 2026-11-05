package se.arbetsprov.ai.llm;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.ai.AskException;
import se.arbetsprov.ai.catalog.OperationCatalog;

/**
 * User story 3: the small LLM fills parameter values only. Ollama is stubbed with WireMock.
 */
@DisplayName("Ollama parameter filler")
class OllamaParameterFillerTests {

    private static final String QUESTION = "lista de fem största länderna i Europa";

    private final WireMockServer ollama = new WireMockServer(options().dynamicPort());
    private final OperationCatalog catalog = OperationCatalog.standard();

    @BeforeEach
    void start() {
        ollama.start();
    }

    @AfterEach
    void stop() {
        ollama.stop();
    }

    private OllamaParameterFiller filler() {
        return new OllamaParameterFiller(URI.create(ollama.baseUrl()), "qwen2.5:1.5b");
    }

    private void answer(String content) {
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"");
        ollama.stubFor(post("/api/chat").willReturn(okJson(
                "{\"model\":\"qwen2.5:1.5b\",\"message\":{\"role\":\"assistant\",\"content\":\"" + escaped + "\"},\"done\":true}")));
    }

    @Test
    void fill_sendsInstructionSchemaAndQuestionWithTemperature0() {
        // Given a stub Ollama and the operation List countries
        answer("{}");

        // When "lista de fem största länderna i Europa" is filled
        filler().fill(catalog.byId("countries.list").orElseThrow(), QUESTION);

        // Then one POST /api/chat was sent with model qwen2.5:1.5b, stream false, temperature 0,
        // the English instruction in the system message, the question in the user message,
        // and a "format" JSON schema with the operation's parameters (language is detected locally)
        ollama.verify(1, postRequestedFor(urlEqualTo("/api/chat"))
                .withRequestBody(matchingJsonPath("$.model", containing("qwen2.5:1.5b")))
                .withRequestBody(matchingJsonPath("$[?(@.stream == false)]"))
                .withRequestBody(matchingJsonPath("$.options[?(@.temperature == 0)]"))
                .withRequestBody(matchingJsonPath("$.messages[0].content", containing("Operation: List countries")))
                .withRequestBody(matchingJsonPath("$.messages[1].content", containing(QUESTION)))
                .withRequestBody(matchingJsonPath("$.format.properties.sort.enum"))
                .withRequestBody(matchingJsonPath("$.format.properties.size")));
    }

    @Test
    void fill_returnsParameterValues() {
        // Given the stub answers {"region":"Europe","sort":"population","direction":"desc","size":5,"page":null}
        answer("{\"region\":\"Europe\",\"sort\":\"population\",\"direction\":\"desc\",\"size\":5,\"page\":null}");

        // When filled
        var filled = filler().fill(catalog.byId("countries.list").orElseThrow(), QUESTION);

        // Then the values are region Europe, sort population, direction desc and size 5; null means not given
        assertThat(filled.understood()).isTrue();
        assertThat(filled.values()).containsOnly(Map.entry("region", "Europe"), Map.entry("sort", "population"),
                Map.entry("direction", "desc"), Map.entry("size", 5));
    }

    @Test
    void fill_whenModelAnswersInvalidJson_reportsNotUnderstood() {
        // Given the stub answers a message whose content is not JSON
        answer("Sure! Here are the countries you asked for.");

        // When filled
        var filled = filler().fill(catalog.byId("countries.list").orElseThrow(), QUESTION);

        // Then a "not understood" result is returned, not an exception
        assertThat(filled.understood()).isFalse();
    }

    @Test
    void fill_whenOllamaUnreachable_failsAsUnreachable() throws Exception {
        // Given an Ollama address where nothing listens
        int port;
        try (var socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        var unreachable = new OllamaParameterFiller(URI.create("http://localhost:" + port), "qwen2.5:1.5b");

        // When filled
        // Then an unreachable error names the address (the CLI exits with code 4)
        assertThatThrownBy(() -> unreachable.fill(catalog.byId("countries.list").orElseThrow(), QUESTION))
                .isInstanceOfSatisfying(AskException.class, e -> {
                    assertThat(e.exitCode()).isEqualTo(4);
                    assertThat(e.getMessage()).contains("localhost:" + port);
                });
    }

    @Test
    void fill_whenModelNotPulled_failsAsMissingPrerequisite() {
        // Given the stub answers 404 {"error":"model 'qwen2.5:1.5b' not found"}
        ollama.stubFor(post("/api/chat").willReturn(aResponse().withStatus(404)
                .withBody("{\"error\":\"model 'qwen2.5:1.5b' not found\"}")));

        // When filled
        // Then a missing-prerequisite error names the model (the CLI exits with code 3)
        assertThatThrownBy(() -> filler().fill(catalog.byId("countries.list").orElseThrow(), QUESTION))
                .isInstanceOfSatisfying(AskException.class, e -> {
                    assertThat(e.exitCode()).isEqualTo(3);
                    assertThat(e.getMessage()).contains("qwen2.5:1.5b");
                });
    }
}
