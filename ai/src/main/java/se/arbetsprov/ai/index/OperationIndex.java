package se.arbetsprov.ai.index;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Finds the operation whose embeddings are closest to a question's embedding. */
public interface OperationIndex {

    /**
     * One vector in the index. An operation may have several (its description and each example);
     * the best-scoring one counts.
     *
     * @param id     operation id
     * @param vector embedding
     */
    record Entry(String id, float[] vector) {
    }

    /** The closest operation, or empty when even the closest is below the threshold. */
    Optional<Match> nearest(float[] query);

    /**
     * Creates an index of {@code kind} over {@code entries}.
     *
     * @param rule when the closest operation counts as a match
     * @throws UnsupportedOperationException for {@link IndexKind#IVF_PQ}, which is documented only
     */
    static OperationIndex create(IndexKind kind, List<Entry> entries, MatchRule rule) {
        return switch (kind) {
            case FLAT -> new FlatIndex(entries, rule);
            case IVF_PQ -> throw new UnsupportedOperationException(
                    "IVF_PQ is documented but not built: it needs training data (thousands of vectors) for its "
                            + "clusters and codebooks, and FLAT is exact and faster for a catalog of this size");
        };
    }

    /** One vector per id, matched by threshold only. */
    static OperationIndex create(IndexKind kind, Map<String, float[]> entries, double threshold) {
        return create(kind, entries.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList(),
                MatchRule.absolute(threshold));
    }
}
