package se.arbetsprov.ai.cli;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.arbetsprov.ai.AskException;
import se.arbetsprov.ai.testing.Stubs;

/**
 * User story 5: --setup checks prerequisites and installs them after Yes/No. Downloads come
 * from WireMock and commands go to a recording fake, so nothing is installed for real.
 */
@DisplayName("Setup")
class SetupTests {

    private static final byte[] MODEL_BYTES = "model bytes".getBytes(StandardCharsets.UTF_8);
    private static final byte[] TOKENIZER_BYTES = "tokenizer bytes".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path cache;

    private final WireMockServer files = new WireMockServer(options().dynamicPort());
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private Stubs.Commands commands;

    @BeforeEach
    void start() {
        files.start();
        files.stubFor(get("/model").willReturn(aResponse().withBody(MODEL_BYTES)));
        files.stubFor(get("/tokenizer").willReturn(aResponse().withBody(TOKENIZER_BYTES)));
        commands = ollamaReady(new Stubs.Commands());
    }

    @AfterEach
    void stop() {
        files.stop();
    }

    private static Stubs.Commands ollamaReady(Stubs.Commands commands) {
        return commands.answer("ollama --version", 0, "ollama version 0.12.0")
                .answer("ollama list", 0, "NAME            ID      SIZE\nqwen2.5:1.5b    abc     986 MB\n");
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private AskContext context(Os os, String modelSha) throws Exception {
        return new AskContext(cache, os, commands, List.of(
                new ModelFile(ModelFile.MODEL, URI.create(files.baseUrl() + "/model"), modelSha, MODEL_BYTES.length),
                new ModelFile(ModelFile.TOKENIZER, URI.create(files.baseUrl() + "/tokenizer"), sha256(TOKENIZER_BYTES),
                        TOKENIZER_BYTES.length)),
                "qwen2.5:1.5b", (m, t) -> {
                    throw new AssertionError("not used");
                });
    }

    private AskContext context(Os os) throws Exception {
        return context(os, sha256(MODEL_BYTES));
    }

    private int setup(AskContext context, String answers) {
        return new Setup(context, new BufferedReader(new StringReader(answers)),
                new PrintStream(output, true, StandardCharsets.UTF_8)).run();
    }

    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }

    private void placeFiles() throws Exception {
        Files.createDirectories(cache.resolve("models"));
        Files.write(cache.resolve("models").resolve(ModelFile.MODEL), MODEL_BYTES);
        Files.write(cache.resolve("models").resolve(ModelFile.TOKENIZER), TOKENIZER_BYTES);
    }

    @Test
    void everythingPresent_asksNothing() throws Exception {
        // Given the model and tokenizer in the cache folder, ollama on the PATH and the model pulled
        placeFiles();

        // When setup runs
        int code = setup(context(Os.WINDOWS), "");

        // Then each item is reported as present and no question is asked
        assertThat(code).isZero();
        assertThat(printed()).contains("[ok] model_quantized.onnx", "[ok] tokenizer.json", "[ok] Ollama", "[ok] qwen2.5:1.5b")
                .doesNotContain("Installera?");
    }

    @Test
    void missingModel_yes_downloadsAndVerifiesChecksum() throws Exception {
        // Given no model file and the answer "Yes"
        // When setup runs
        setup(context(Os.WINDOWS), "Yes\nYes\n");

        // Then the file is downloaded from the configured address into the cache folder and its
        // SHA-256 matches the pinned value
        Path model = cache.resolve("models").resolve(ModelFile.MODEL);
        assertThat(model).hasBinaryContent(MODEL_BYTES);
        assertThat(Setup.sha256(model)).isEqualTo(sha256(MODEL_BYTES));
        assertThat(printed()).contains("model_quantized.onnx hämtad och kontrollerad");
    }

    @Test
    void checksumMismatch_deletesFileAndFails() throws Exception {
        // Given a download whose content does not match the pinned SHA-256
        var context = context(Os.WINDOWS, "0".repeat(64));

        // When setup runs
        // Then the partial file is removed and setup fails naming the file
        assertThatThrownBy(() -> setup(context, "yes\n"))
                .isInstanceOfSatisfying(AskException.class, e -> {
                    assertThat(e.exitCode()).isEqualTo(3);
                    assertThat(e.getMessage()).contains("model_quantized.onnx");
                });
        try (var left = Files.list(cache.resolve("models"))) {
            assertThat(left).isEmpty();
        }
    }

    @Test
    void answers_onlyYesOrNoInEitherCaseAreAccepted() throws Exception {
        // Given the answers "y", "ja", "YES" and then "yes"
        // When setup asks about a missing item
        setup(context(Os.WINDOWS), "y\nja\nYES\nyes\nno\n");

        // Then it asks again after each of the first three and accepts "yes"
        assertThat(printed().split("Svara Yes eller No\\.", -1)).hasSize(4);
        assertThat(cache.resolve("models").resolve(ModelFile.MODEL)).exists();
    }

    @Test
    void no_printsManualStepAndContinuesWithNextItem() throws Exception {
        // Given a missing tokenizer and the answer "no"
        Files.createDirectories(cache.resolve("models"));
        Files.write(cache.resolve("models").resolve(ModelFile.MODEL), MODEL_BYTES);

        // When setup runs
        setup(context(Os.WINDOWS), "no\n");

        // Then the download address and target folder are printed and the next item is checked
        assertThat(printed()).contains("Hoppar över. Ladda ner manuellt: " + files.baseUrl() + "/tokenizer till "
                + cache.resolve("models")).contains("[ok] Ollama");
    }

    @Test
    void missingOllama_yesOnWindows_runsWinget() throws Exception {
        // Given Windows, no ollama on the PATH and the answer "Yes"
        placeFiles();
        commands = new Stubs.Commands();

        // When setup runs
        setup(context(Os.WINDOWS), "Yes\n");

        // Then "winget install Ollama.Ollama" was run
        assertThat(commands.ran).anyMatch(c -> c.startsWith("winget install Ollama.Ollama"));
    }

    @Test
    void missingOllama_onLinux_printsInstallCommandInsteadOfRunningIt() throws Exception {
        // Given Linux and no ollama on the PATH
        placeFiles();
        commands = new Stubs.Commands();

        // When setup runs
        setup(context(Os.LINUX), "");

        // Then the official install command is printed and nothing is run
        assertThat(printed()).contains("curl -fsSL https://ollama.com/install.sh | sh");
        assertThat(commands.ran).containsExactly("ollama --version");
    }

    @Test
    void missingOllamaModel_yes_runsPull() throws Exception {
        // Given ollama present but qwen2.5:1.5b not in "ollama list", and the answer "Yes"
        placeFiles();
        commands = new Stubs.Commands().answer("ollama --version", 0, "ollama version 0.12.0")
                .answer("ollama list", 0, "NAME    ID    SIZE\n");

        // When setup runs
        setup(context(Os.WINDOWS), "Yes\n");

        // Then "ollama pull qwen2.5:1.5b" was run
        assertThat(commands.ran).contains("ollama pull qwen2.5:1.5b");
    }
}
