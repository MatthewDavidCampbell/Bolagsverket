package se.arbetsprov.ai.index;

/**
 * When the closest operation counts as a match.
 *
 * <p>A score at or above {@code threshold} always matches. Just below it, down to
 * {@code lowerThreshold}, a question still matches if it clearly prefers one operation: the best
 * operation must beat the second best by at least {@code margin}. Unrelated text tends to score
 * about the same for every operation, so a small gap is what separates "a little unsure" from
 * "about something else".
 *
 * @param threshold      score that always matches
 * @param lowerThreshold lowest score that can match with a clear winner
 * @param margin         smallest lead over the second-best operation below {@code threshold}
 */
public record MatchRule(double threshold, double lowerThreshold, double margin) {

    /** Measured with multilingual-e5-small on the catalog (see README). */
    public static final MatchRule DEFAULT = new MatchRule(0.80, 0.77, 0.015);

    public MatchRule {
        if (lowerThreshold > threshold) {
            throw new IllegalArgumentException("lowerThreshold must not be above threshold");
        }
    }

    /** Only the threshold counts. */
    public static MatchRule absolute(double threshold) {
        return new MatchRule(threshold, threshold, 0);
    }

    /** Whether {@code best}, leading {@code second} (or -1 when there is none), is a match. */
    public boolean accepts(double best, double second) {
        return best >= threshold || (best >= lowerThreshold && best - second >= margin);
    }
}
