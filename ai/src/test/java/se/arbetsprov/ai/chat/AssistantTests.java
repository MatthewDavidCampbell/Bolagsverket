package se.arbetsprov.ai.chat;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.ai.AskException;
import se.arbetsprov.ai.catalog.OperationCatalog;
import se.arbetsprov.ai.index.MatchRule;
import se.arbetsprov.ai.service.ServiceClient;
import se.arbetsprov.ai.testing.Stubs;

/**
 * User story 1: the whole chat turn. The embedding model and the parameter filler are stubs;
 * the service is WireMock. Each test reads input lines and checks the printed transcript.
 */
@DisplayName("Assistant")
class AssistantTests {

    private static final String LIST_SV = "lista de fem största länderna i Europa";
    private static final String LIST_EN = "show the five largest countries in Europe";
    private static final String MARK_SV = "markera Sverige som besökt, anteckning: midsommar";
    private static final String TOP_EUROPE = """
            {"items":[{"code":"RUS","name":"Russia","population":146028325},
                      {"code":"DEU","name":"Germany","population":83298787}],
             "page":0,"size":5,"totalItems":53,"totalPages":11}""";

    private final WireMockServer service = new WireMockServer(options().dynamicPort());
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final Stubs.Embeddings embeddings = new Stubs.Embeddings()
            .route(LIST_SV, "countries.list").route(LIST_EN, "countries.list").route(MARK_SV, "visits.create");
    private final Stubs.Filler filler = new Stubs.Filler()
            .answer(LIST_SV, Map.of("region", "Europe", "sort", "population", "direction", "desc", "size", 5))
            .answer(LIST_EN, Map.of("region", "Europe", "sort", "population", "direction", "desc", "size", 5))
            .answer(MARK_SV, Map.of("countryCode", "SWE", "note", "midsommar"));

    @BeforeEach
    void start() {
        service.start();
        service.stubFor(get(urlEqualTo("/api/countries?region=Europe&sort=population&direction=desc&size=5"))
                .willReturn(okJson(TOP_EUROPE)));
    }

    @AfterEach
    void stop() {
        service.stop();
    }

    private int chat(String input, boolean json, URI serviceUri) {
        var assistant = new Assistant(OperationCatalog.standard(), embeddings, filler, new ServiceClient(serviceUri),
                new Transcript(new PrintStream(output, true, StandardCharsets.UTF_8), "alice", true),
                "alice", Language.SV, json, MatchRule.DEFAULT);
        return assistant.run(new BufferedReader(new StringReader(input)));
    }

    private int chat(String input) {
        return chat(input, false, URI.create(service.baseUrl()));
    }

    private String transcript() {
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void start_greetsInSwedishWithChatPrefix() {
        // Given user "alice" and language sv
        // When the chat starts
        chat("hej\n");

        // Then the first line is "(chat) Vad vill du göra?" and input is echoed as "(alice) ..."
        assertThat(transcript()).startsWith("(chat) Vad vill du göra?").contains("(alice) hej");
    }

    @Test
    void listCountriesInSwedish_callsServiceAndPrintsNumberedList() {
        // Given a Swedish question matching List countries and filled values region Europe,
        // sort population, direction desc, size 5
        // When the turn runs
        chat(LIST_SV + "\n");

        // Then "(chat) GET /api/countries?region=Europe&sort=population&direction=desc&size=5"
        // is printed, the stub service was called exactly so, and a numbered Swedish list
        // "1. Russia – 146 028 325 invånare" follows
        assertThat(transcript())
                .contains("(chat) GET /api/countries?region=Europe&sort=population&direction=desc&size=5")
                .contains("(chat) 1. Russia – 146 028 325 invånare")
                .contains("(chat) 2. Germany – 83 298 787 invånare")
                .contains("(chat) Sida 1 av 11, 53 länder totalt.");
        service.verify(1, getRequestedFor(urlEqualTo("/api/countries?region=Europe&sort=population&direction=desc&size=5")));
    }

    @Test
    void questionInEnglish_answersInEnglish() {
        // Given a question written in English
        // When the turn runs
        chat(LIST_EN + "\n");

        // Then the reply text is English, e.g. "1. Russia – 146,028,325 people"
        assertThat(transcript()).contains("(chat) 1. Russia – 146,028,325 people", "Page 1 of 11");
    }

    @Test
    void markVisitedDirective_postsWithUserHeaderAndConfirms() {
        // Given a directive matching Mark country as visited, values countryCode SWE, note midsommar
        service.stubFor(post("/api/visits").willReturn(aResponse().withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"countryCode\":\"SWE\",\"countryName\":\"Sweden\",\"note\":\"midsommar\"}")));

        // When the turn runs
        chat(MARK_SV + "\n");

        // Then the stub service received POST /api/visits with X-User-Id alice and the JSON body,
        // and "(chat) Sweden är markerat som besökt." is printed
        service.verify(postRequestedFor(urlEqualTo("/api/visits"))
                .withHeader("X-User-Id", equalTo("alice"))
                .withRequestBody(equalToJson("{\"countryCode\":\"SWE\",\"note\":\"midsommar\"}")));
        assertThat(transcript()).contains("(chat) Sweden är markerat som besökt.");
    }

    @Test
    void serviceAnswers409_printsProblemAndContinues() {
        // Given the stub service answers 409 with a problem detail
        service.stubFor(post("/api/visits").willReturn(aResponse().withStatus(409)
                .withHeader("Content-Type", "application/problem+json")
                .withBody("{\"status\":409,\"detail\":\"Country SWE is already marked as visited\"}")));

        // When the turn runs
        int code = chat(MARK_SV + "\nhej\n");

        // Then the detail is printed as "(chat) ..." and the next prompt follows
        assertThat(transcript()).contains("(chat) Country SWE is already marked as visited").contains("(alice) hej");
        assertThat(code).isZero();
    }

    @Test
    void lowMatchScore_saysNotUnderstoodWithoutCallingAnything() {
        // Given a question whose best score is below the threshold
        // When the turn runs
        chat("vad blir vädret i morgon?\n");

        // Then "(chat) Jag förstod inte, försök igen." is printed and neither the filler nor the
        // service was called
        assertThat(transcript()).contains("(chat) Jag förstod inte, försök igen.");
        assertThat(filler.asked).isEmpty();
        service.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void jsonOption_printsRawResponse() {
        // Given --json
        // When a List countries turn runs
        chat(LIST_SV + "\n", true, URI.create(service.baseUrl()));

        // Then the service's JSON body is printed as returned
        assertThat(transcript()).contains(TOP_EUROPE).doesNotContain("invånare");
    }

    @Test
    void languageCommands_switchReplies() {
        // Given the input "/en" and then a question written in Swedish
        // When the turns run
        chat("/en\n" + LIST_SV + "\n");

        // Then replies are in English for the rest of the session
        assertThat(transcript()).contains("(chat) I will answer in English.", "146,028,325 people");
    }

    @Test
    void exitCommandOrEndOfInput_endsWithCode0() {
        // Given the input "/exit", and separately end of input
        // When the chat runs
        // Then it ends normally with exit code 0
        assertThat(chat("/exit\n" + LIST_SV + "\n")).isZero();
        assertThat(filler.asked).isEmpty();
        assertThat(chat("")).isZero();
    }

    @Test
    void serviceUnreachable_failsFastWithCode4() throws Exception {
        // Given a service address where nothing listens
        int port;
        try (var socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        // When a turn needs the service
        // Then the CLI exits with code 4 naming the address
        URI down = URI.create("http://localhost:" + port);
        assertThatThrownBy(() -> chat(LIST_SV + "\n", false, down))
                .isInstanceOfSatisfying(AskException.class, e -> {
                    assertThat(e.exitCode()).isEqualTo(4);
                    assertThat(e.getMessage()).contains(down.toString());
                });
    }
}
