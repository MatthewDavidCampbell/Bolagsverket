package se.arbetsprov.ai.cli;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import se.arbetsprov.ai.AskException;
import se.arbetsprov.ai.catalog.OperationCatalog;
import se.arbetsprov.ai.chat.Assistant;
import se.arbetsprov.ai.chat.Transcript;
import se.arbetsprov.ai.index.MatchRule;
import se.arbetsprov.ai.llm.OllamaParameterFiller;
import se.arbetsprov.ai.service.ServiceClient;

/**
 * {@code ask}: a chat in Swedish or English that turns questions and directives into calls to
 * the countries and visits service. Two phases: {@code ask --setup} once, then
 * {@code ask --user <id>}. Every failure ends the CLI with its exit code (see {@link AskException}).
 */
public final class AskCli {

    private AskCli() {
    }

    public static void main(String[] args) {
        // A terminal keeps its own encoding; piped input and output are UTF-8 (Git Bash, CI, files).
        var console = System.console();
        Charset charset = console != null ? console.charset() : StandardCharsets.UTF_8;
        var in = new BufferedReader(new InputStreamReader(System.in, charset));
        var out = console != null ? System.out : new PrintStream(new FileOutputStream(FileDescriptor.out), true, charset);
        System.exit(run(args, in, out, AskContext.defaults(), console == null));
    }

    /**
     * Runs the CLI and returns its exit code.
     *
     * @param echoInput echo the user's lines (when input is piped rather than typed)
     */
    public static int run(String[] args, BufferedReader in, PrintStream out, AskContext context, boolean echoInput) {
        try {
            var options = CliOptions.parse(args);
            if (options.help()) {
                out.println(CliOptions.USAGE);
                return 0;
            }
            if (options.setup()) {
                return new Setup(context, in, out).run();
            }

            // Phase 2 fails fast before anything is loaded or greeted.
            Prerequisites.requireReady(context, options.ollama());
            var embeddings = context.embeddings().create(
                    context.modelsDir().resolve(ModelFile.MODEL), context.modelsDir().resolve(ModelFile.TOKENIZER));
            var assistant = new Assistant(
                    OperationCatalog.standard(),
                    embeddings,
                    new OllamaParameterFiller(options.ollama(), context.ollamaModel()),
                    new ServiceClient(options.service()),
                    new Transcript(out, options.user(), echoInput),
                    options.user(),
                    options.lang(),
                    options.json(),
                    MatchRule.DEFAULT);
            return assistant.run(in);
        } catch (AskException e) {
            out.println(e.getMessage());
            return e.exitCode();
        }
    }
}
