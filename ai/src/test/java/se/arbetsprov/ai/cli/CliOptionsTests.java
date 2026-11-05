package se.arbetsprov.ai.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.arbetsprov.ai.AskException;
import se.arbetsprov.ai.chat.Language;
import se.arbetsprov.ai.testing.Stubs;

/**
 * User story 4: options, defaults and fail-fast usage errors. Nothing is loaded before the
 * options are valid.
 */
@DisplayName("CLI options")
class CliOptionsTests {

    @TempDir
    Path cache;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private int run(String... args) {
        // The embedding factory fails the test if anything tries to load the model.
        var context = new AskContext(cache, Os.WINDOWS, new Stubs.Commands(), ModelFile.defaults(), "qwen2.5:1.5b",
                (model, tokenizer) -> {
                    throw new AssertionError("model loaded");
                });
        return AskCli.run(args, new BufferedReader(new StringReader("")),
                new PrintStream(output, true, StandardCharsets.UTF_8), context, true);
    }

    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void help_printsUsageWithDefaultsAndExits0() {
        // Given the argument --help
        // When the CLI runs
        int code = run("--help");

        // Then usage is printed listing --user (required), --service (default http://localhost:8080),
        // --lang (default sv), --json and --setup, and the exit code is 0
        assertThat(code).isZero();
        assertThat(printed()).contains("--user", "krävs", "--service", "standard: http://localhost:8080",
                "--lang", "standard: sv", "--json", "--setup");
    }

    @Test
    void missingOrBlankUser_exits2WithExplanation() {
        // Given no --user, and separately --user "  "
        // When the CLI runs
        int missing = run();
        int blank = run("--user", "  ");

        // Then it exits with code 2 and prints "Fel: --user saknas. Besök sparas per användare,
        // ange t.ex. --user alice." without loading the model or calling anything
        assertThat(missing).isEqualTo(2);
        assertThat(blank).isEqualTo(2);
        assertThat(printed()).contains("Fel: --user saknas. Besök sparas per användare, ange t.ex. --user alice.");
    }

    @Test
    void defaults_areServiceLocalhost8080AndSwedish() {
        // Given only --user alice
        // When the options are parsed
        var options = CliOptions.parse("--user", "alice");

        // Then the service is http://localhost:8080, the language is sv and --json is off
        assertThat(options.service()).isEqualTo(URI.create("http://localhost:8080"));
        assertThat(options.lang()).isEqualTo(Language.SV);
        assertThat(options.json()).isFalse();
    }

    @Test
    void explicitOptions_overrideDefaults() {
        // Given --user bob --service http://example.test:9090 --lang en --json
        // When the options are parsed
        var options = CliOptions.parse("--user", "bob", "--service", "http://example.test:9090", "--lang", "en", "--json");

        // Then each value is taken as given
        assertThat(options.user()).isEqualTo("bob");
        assertThat(options.service()).isEqualTo(URI.create("http://example.test:9090"));
        assertThat(options.lang()).isEqualTo(Language.EN);
        assertThat(options.json()).isTrue();
    }

    @Test
    void unknownOptionOrUnsupportedLanguage_exits2WithUsage() {
        // Given --verbose, and separately --lang de
        // When the CLI runs
        int unknown = run("--user", "alice", "--verbose");
        int language = run("--user", "alice", "--lang", "de");

        // Then it exits with code 2, naming the problem and printing usage
        assertThat(unknown).isEqualTo(2);
        assertThat(language).isEqualTo(2);
        assertThat(printed()).contains("Okänt alternativ: --verbose", "Språket måste vara sv eller en, inte de", "Användning:");
    }

    @Test
    void setup_doesNotRequireUser() {
        // Given only --setup
        // When the options are parsed
        var options = CliOptions.parse("--setup");

        // Then they are valid: setup installs prerequisites and needs no user
        assertThat(options.setup()).isTrue();
        assertThatThrownBy(() -> CliOptions.parse("--user"))
                .isInstanceOfSatisfying(AskException.class, e -> assertThat(e.exitCode()).isEqualTo(2));
    }
}
