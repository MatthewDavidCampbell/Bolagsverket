package se.arbetsprov.ai.llm;

import java.util.Map;

/**
 * Parameter values the LLM found in a question.
 *
 * @param values     parameter name to suggested value (not yet validated)
 * @param understood false when the LLM's answer could not be read
 */
public record Filled(Map<String, Object> values, boolean understood) {

    public Filled {
        values = Map.copyOf(values);
    }

    public static Filled of(Map<String, Object> values) {
        return new Filled(values, true);
    }

    public static Filled notUnderstood() {
        return new Filled(Map.of(), false);
    }
}
