package se.arbetsprov.ai.cli;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.arbetsprov.ai.testing.Stubs;

/**
 * The two phases share one check: --setup decides what to install from it, and a normal
 * "ask --user" fails fast from it before the chat starts. Uses a temporary cache folder and a
 * WireMock Ollama.
 */
@DisplayName("Prerequisites")
class PrerequisitesTests {

    @TempDir
    Path cache;

    private final WireMockServer ollama = new WireMockServer(options().dynamicPort());
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final List<ModelFile> files = List.of(
            new ModelFile(ModelFile.MODEL, URI.create("http://files.test/model"), "a".repeat(64), 1),
            new ModelFile(ModelFile.TOKENIZER, URI.create("http://files.test/tokenizer"), "b".repeat(64), 1));

    @BeforeEach
    void start() {
        ollama.start();
    }

    @AfterEach
    void stop() {
        ollama.stop();
    }

    private AskContext context() {
        return new AskContext(cache, Os.WINDOWS, new Stubs.Commands(), files, "qwen2.5:1.5b",
                (model, tokenizer) -> new Stubs.Embeddings());
    }

    private void ollamaHas(String... models) {
        var names = new StringBuilder();
        for (String m : models) {
            names.append(names.isEmpty() ? "" : ",").append("{\"name\":\"").append(m).append("\"}");
        }
        ollama.stubFor(get("/api/tags").willReturn(okJson("{\"models\":[" + names + "]}")));
    }

    private void filesSetUp() throws Exception {
        Files.createDirectories(cache.resolve("models"));
        Files.writeString(cache.resolve("models").resolve(ModelFile.MODEL), "model");
        Files.writeString(cache.resolve("models").resolve(ModelFile.TOKENIZER), "tokenizer");
        Prerequisites.writeMarker(context(), Map.of(ModelFile.MODEL, "a".repeat(64), ModelFile.TOKENIZER, "b".repeat(64)));
    }

    private int ask(String input, URI ollamaUri, String... args) {
        String[] all = new String[args.length + 2];
        System.arraycopy(args, 0, all, 0, args.length);
        all[args.length] = "--ollama";
        all[args.length + 1] = ollamaUri.toString();
        return AskCli.run(all, new BufferedReader(new StringReader(input)),
                new PrintStream(output, true, StandardCharsets.UTF_8), context(), true);
    }

    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void ask_withEverythingInPlace_startsTheChat() throws Exception {
        // Given a marker file matching the model and tokenizer, and Ollama listing qwen2.5:1.5b
        filesSetUp();
        ollamaHas("qwen2.5:1.5b");

        // When "ask --user alice" starts
        int code = ask("/exit\n", URI.create(ollama.baseUrl()), "--user", "alice");

        // Then the check passes without re-hashing the files and "(chat) Vad vill du göra?" is printed
        assertThat(code).isZero();
        assertThat(printed()).startsWith("(chat) Vad vill du göra?");
    }

    @Test
    void ask_withoutSetup_exits3ListingWhatIsMissing() {
        // Given an empty cache folder and Ollama without the model
        ollamaHas("llama3.2:1b");

        // When "ask --user alice" starts
        int code = ask("", URI.create(ollama.baseUrl()), "--user", "alice");

        // Then it exits with code 3 printing "Saknas: model_quantized.onnx, tokenizer.json,
        // qwen2.5:1.5b. Kör först: ask --setup" before any greeting
        assertThat(code).isEqualTo(3);
        assertThat(printed()).contains("Saknas: model_quantized.onnx, tokenizer.json, qwen2.5:1.5b. Kör först: ask --setup")
                .doesNotContain("Vad vill du göra?");
    }

    @Test
    void ask_whenFileChangedSinceSetup_asksToRunSetupAgain() throws Exception {
        // Given a marker file whose recorded size or modification time no longer matches the model
        filesSetUp();
        ollamaHas("qwen2.5:1.5b");
        Path model = cache.resolve("models").resolve(ModelFile.MODEL);
        Files.setLastModifiedTime(model, FileTime.fromMillis(Files.getLastModifiedTime(model).toMillis() + 60_000));

        // When "ask --user alice" starts
        int code = ask("", URI.create(ollama.baseUrl()), "--user", "alice");

        // Then it exits with code 3 naming the file and suggesting "ask --setup"
        assertThat(code).isEqualTo(3);
        assertThat(printed()).contains("Ändrad sedan setup: model_quantized.onnx", "ask --setup");
    }

    @Test
    void ask_whenOllamaNotRunning_exits4NamingTheAddress() throws Exception {
        // Given valid files but nothing listening on the Ollama address
        filesSetUp();
        int port;
        try (var socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        URI down = URI.create("http://localhost:" + port);

        // When "ask --user alice" starts
        int code = ask("", down, "--user", "alice");

        // Then it exits with code 4 naming the address and suggesting to start Ollama
        assertThat(code).isEqualTo(4);
        assertThat(printed()).contains(down.toString(), "Starta Ollama");
    }

    @Test
    void setup_afterVerifyingFiles_writesMarker() throws Exception {
        // Given setup has downloaded and verified the model and tokenizer
        filesSetUp();

        // When it finishes
        var marker = Prerequisites.readMarker(context());

        // Then setup.json in the cache folder records each file's SHA-256, size and modification time
        assertThat(cache.resolve("setup.json")).exists();
        assertThat(marker).containsOnlyKeys(ModelFile.MODEL, ModelFile.TOKENIZER);
        assertThat(marker.get(ModelFile.MODEL)).containsEntry("sha256", "a".repeat(64))
                .containsKeys("size", "modified");
    }

    @Test
    void setup_whenUserDeclinesAnItem_doesNotWriteMarkerForIt() throws Exception {
        // Given the answer "no" for the tokenizer
        Files.createDirectories(cache.resolve("models"));
        Prerequisites.writeMarker(context(), Map.of());
        ollamaHas("qwen2.5:1.5b");
        var setup = new Setup(context(), new BufferedReader(new StringReader("no\nno\n")),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));

        // When setup finishes
        setup.run();

        // Then the marker does not list the tokenizer, and a later "ask --user" reports it missing
        assertThat(setup.marker()).doesNotContainKey(ModelFile.TOKENIZER);
        int code = ask("", URI.create(ollama.baseUrl()), "--user", "alice");
        assertThat(code).isEqualTo(3);
        assertThat(printed()).contains("tokenizer.json");
    }

    @Test
    void setupAndUserTogether_exits2() {
        // Given --setup --user alice
        // When the CLI runs
        int code = ask("", URI.create(ollama.baseUrl()), "--setup", "--user", "alice");

        // Then it exits with code 2: "Välj antingen --setup eller --user"
        assertThat(code).isEqualTo(2);
        assertThat(printed()).contains("Välj antingen --setup eller --user");
    }
}
