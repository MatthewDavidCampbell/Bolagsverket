package se.arbetsprov.ai.index;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Exact nearest-neighbour search by cosine similarity. Every vector is normalised to length 1 once,
 * so the cosine is a plain dot product; the question is compared with every entry. Each operation
 * scores its best entry, and the {@link MatchRule} decides whether the winner counts.
 */
final class FlatIndex implements OperationIndex {

    private final List<Entry> entries;
    private final MatchRule rule;

    FlatIndex(List<Entry> vectors, MatchRule rule) {
        this.rule = rule;
        int dimensions = vectors.isEmpty() ? -1 : vectors.getFirst().vector().length;
        for (Entry e : vectors) {
            if (e.vector().length != dimensions) {
                throw new IllegalArgumentException("All vectors must have the same dimensions: " + e.id()
                        + " has " + e.vector().length + ", expected " + dimensions);
            }
        }
        this.entries = vectors.stream().map(e -> new Entry(e.id(), normalise(e.vector()))).toList();
    }

    @Override
    public Optional<Match> nearest(float[] query) {
        float[] q = normalise(query);
        var bestPerOperation = new HashMap<String, Double>();
        for (Entry entry : entries) {
            bestPerOperation.merge(entry.id(), dot(q, entry.vector()), Math::max);
        }

        Match best = null;
        double second = -1;
        for (var e : bestPerOperation.entrySet()) {
            if (best == null || e.getValue() > best.score()) {
                if (best != null) {
                    second = best.score();
                }
                best = new Match(e.getKey(), e.getValue());
            } else if (e.getValue() > second) {
                second = e.getValue();
            }
        }
        return best != null && rule.accepts(best.score(), second) ? Optional.of(best) : Optional.empty();
    }

    private static double dot(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Query has " + a.length + " dimensions, index has " + b.length);
        }
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private static float[] normalise(float[] v) {
        double length = 0;
        for (float x : v) {
            length += x * x;
        }
        length = Math.sqrt(length);
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            out[i] = length == 0 ? 0 : (float) (v[i] / length);
        }
        return out;
    }
}
