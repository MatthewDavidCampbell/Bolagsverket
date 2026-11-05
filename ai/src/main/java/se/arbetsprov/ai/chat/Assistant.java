package se.arbetsprov.ai.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import se.arbetsprov.ai.catalog.InvalidParametersException;
import se.arbetsprov.ai.catalog.Operation;
import se.arbetsprov.ai.catalog.OperationCatalog;
import se.arbetsprov.ai.catalog.RestCall;
import se.arbetsprov.ai.embedding.EmbeddingModel;
import se.arbetsprov.ai.index.IndexKind;
import se.arbetsprov.ai.index.MatchRule;
import se.arbetsprov.ai.index.OperationIndex;
import se.arbetsprov.ai.llm.ParameterFiller;
import se.arbetsprov.ai.service.ServiceClient;

/**
 * One chat session. Each turn:
 * <ol>
 *   <li>embeds the question and finds the closest catalog operation in the index,</li>
 *   <li>asks the LLM to fill that operation's parameters,</li>
 *   <li>builds the request from the catalog (validating every value) and calls the service,</li>
 *   <li>shows the answer in the question's language.</li>
 * </ol>
 * Answers from the service, including 4xx, are part of the conversation; a service or Ollama
 * that does not answer ends the session with an {@link se.arbetsprov.ai.AskException}.
 */
public final class Assistant {

    private final OperationCatalog catalog;
    private final OperationIndex index;
    private final EmbeddingModel embeddings;
    private final ParameterFiller filler;
    private final ServiceClient service;
    private final Transcript transcript;
    private final ResponseRenderer renderer = new ResponseRenderer();
    private final String user;
    private final boolean json;

    private Language language;
    private Language forced;

    public Assistant(OperationCatalog catalog, EmbeddingModel embeddings, ParameterFiller filler,
                     ServiceClient service, Transcript transcript, String user, Language start,
                     boolean json, MatchRule rule) {
        this.catalog = catalog;
        this.embeddings = embeddings;
        this.filler = filler;
        this.service = service;
        this.transcript = transcript;
        this.user = user;
        this.language = start;
        this.json = json;

        this.index = OperationIndex.create(IndexKind.FLAT, entries(catalog, embeddings), rule);
    }

    /**
     * One vector per operation description ({@code passage:}) and one per English example
     * ({@code query:}, since an example is phrased like a question).
     */
    public static List<OperationIndex.Entry> entries(OperationCatalog catalog, EmbeddingModel embeddings) {
        var entries = new ArrayList<OperationIndex.Entry>();
        for (Operation o : catalog.operations()) {
            entries.add(new OperationIndex.Entry(o.id(), embeddings.embedPassage(o.passage())));
            o.examples().forEach(e -> entries.add(new OperationIndex.Entry(o.id(), embeddings.embedQuery(e))));
        }
        return entries;
    }

    /** Runs until {@code /exit} or end of input; returns the exit code (0). */
    public int run(BufferedReader in) {
        transcript.chat(Messages.greeting(language));
        while (true) {
            transcript.prompt();
            String line = readLine(in);
            if (line == null) {
                return 0;
            }
            transcript.typed(line);
            String input = line.strip();
            if (input.isEmpty()) {
                continue;
            }
            if (input.equals("/exit")) {
                return 0;
            }
            if (input.equals("/sv") || input.equals("/en")) {
                forced = Language.parse(input.substring(1)).orElseThrow();
                language = forced;
                transcript.chat(Messages.switched(language));
                continue;
            }
            turn(input);
        }
    }

    private void turn(String question) {
        if (forced == null) {
            language = LanguageDetector.detect(question).orElse(language);
        }
        var match = index.nearest(embeddings.embedQuery(question));
        if (match.isEmpty()) {
            transcript.chat(Messages.notUnderstood(language));
            return;
        }
        var operation = catalog.byId(match.get().id()).orElseThrow();

        var filled = filler.fill(operation, question);
        if (!filled.understood()) {
            transcript.chat(Messages.notUnderstood(language));
            return;
        }

        var call = buildOrExplain(operation, filled.values());
        if (call == null) {
            return;
        }
        transcript.chat(call.display());
        var response = service.send(call);
        if (json) {
            transcript.raw(response.body());
            return;
        }
        renderer.render(call, response, language).forEach(transcript::chat);
    }

    private RestCall buildOrExplain(Operation operation, Map<String, Object> values) {
        try {
            return catalog.build(operation, values, user);
        } catch (InvalidParametersException e) {
            transcript.chat(Messages.invalidValues(language, e.getMessage()));
            return null;
        }
    }

    private static String readLine(BufferedReader in) {
        try {
            return in.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
