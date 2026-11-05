package se.arbetsprov.ai.llm;

import se.arbetsprov.ai.catalog.Operation;

/** Fills an already chosen operation's parameters from a question. Never chooses the operation. */
public interface ParameterFiller {

    Filled fill(Operation operation, String question);
}
