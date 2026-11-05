package se.arbetsprov.ai.index;

/**
 * The closest entry and how close it is.
 *
 * @param id    entry id, for example "countries.list"
 * @param score cosine similarity, from -1 to 1
 */
public record Match(String id, double score) {
}
