package se.arbetsprov.ai.testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import se.arbetsprov.ai.catalog.Operation;
import se.arbetsprov.ai.catalog.OperationCatalog;
import se.arbetsprov.ai.cli.CommandRunner;
import se.arbetsprov.ai.embedding.EmbeddingModel;
import se.arbetsprov.ai.llm.Filled;
import se.arbetsprov.ai.llm.ParameterFiller;

/** Test doubles for the parts that need a model, Ollama or processes. */
public final class Stubs {

    private Stubs() {
    }

    /**
     * Each catalog operation gets its own axis; a known question points straight along its
     * operation's axis (score 1), anything else points equally at all (score about 0.41).
     */
    public static final class Embeddings implements EmbeddingModel {

        private final List<String> passages = new ArrayList<>();
        private final Map<String, String> questionToOperation = new HashMap<>();
        private final OperationCatalog catalog = OperationCatalog.standard();

        public Embeddings() {
            catalog.operations().forEach(o -> passages.add(o.passage()));
            // Catalog examples point along their own operation's axis, like its description.
            catalog.operations().forEach(o -> o.examples().forEach(e -> questionToOperation.put(e, o.id())));
        }

        public Embeddings route(String question, String operationId) {
            questionToOperation.put(question, operationId);
            return this;
        }

        @Override
        public float[] embedQuery(String text) {
            String id = questionToOperation.get(text);
            if (id == null) {
                float[] v = new float[passages.size()];
                java.util.Arrays.fill(v, 1f);
                return v;
            }
            Operation op = catalog.byId(id).orElseThrow();
            return axis(passages.indexOf(op.passage()));
        }

        @Override
        public float[] embedPassage(String text) {
            return axis(passages.indexOf(text));
        }

        private float[] axis(int i) {
            float[] v = new float[passages.size()];
            v[i] = 1f;
            return v;
        }
    }

    /** Returns prepared values per question and remembers what it was asked. */
    public static final class Filler implements ParameterFiller {

        private final Map<String, Filled> answers = new HashMap<>();
        public final List<String> asked = new ArrayList<>();

        public Filler answer(String question, Map<String, Object> values) {
            answers.put(question, Filled.of(values));
            return this;
        }

        @Override
        public Filled fill(Operation operation, String question) {
            asked.add(question);
            return answers.getOrDefault(question, Filled.notUnderstood());
        }
    }

    /** Records commands and answers from a prepared table. */
    public static final class Commands implements CommandRunner {

        private final Map<String, Result> results = new HashMap<>();
        public final List<String> ran = new ArrayList<>();

        public Commands answer(String command, int exitCode, String output) {
            results.put(command, new Result(exitCode, output));
            return this;
        }

        @Override
        public Result run(List<String> command) {
            String line = String.join(" ", command);
            ran.add(line);
            return results.getOrDefault(line, new Result(-1, "not found"));
        }

        @Override
        public int runVisible(List<String> command) {
            String line = String.join(" ", command);
            ran.add(line);
            return results.getOrDefault(line, new Result(0, "")).exitCode();
        }
    }
}
