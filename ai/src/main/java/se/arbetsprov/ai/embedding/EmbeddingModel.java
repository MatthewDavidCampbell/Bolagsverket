package se.arbetsprov.ai.embedding;

/**
 * Turns text into a vector so texts with similar meaning end up close together, whatever their
 * language. Questions and catalog entries are embedded differently, as e5 models expect.
 */
public interface EmbeddingModel {

    /** Embeds a user's question or directive. */
    float[] embedQuery(String text);

    /** Embeds a catalog entry. */
    float[] embedPassage(String text);
}
