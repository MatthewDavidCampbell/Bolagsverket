package se.arbetsprov.ai.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * User story 2: the hand-coded FLAT index. Uses small fixed vectors, no model.
 */
@DisplayName("FLAT index")
class FlatIndexTests {

    private static Map<String, float[]> entries(Object... idAndVector) {
        var map = new LinkedHashMap<String, float[]>();
        for (int i = 0; i < idAndVector.length; i += 2) {
            map.put((String) idAndVector[i], (float[]) idAndVector[i + 1]);
        }
        return map;
    }

    @Test
    void nearest_returnsEntryWithHighestCosineSimilarityAndItsScore() {
        // Given entries A = (1, 0, 0), B = (0, 1, 0), C = (0.7, 0.7, 0)
        var index = OperationIndex.create(IndexKind.FLAT, entries(
                "A", new float[] {1, 0, 0}, "B", new float[] {0, 1, 0}, "C", new float[] {0.7f, 0.7f, 0}), 0.5);

        // When the query (0.9, 0.1, 0) is searched
        var match = index.nearest(new float[] {0.9f, 0.1f, 0});

        // Then A is returned with a score of about 0.994
        assertThat(match).hasValueSatisfying(m -> {
            assertThat(m.id()).isEqualTo("A");
            assertThat(m.score()).isCloseTo(0.994, within(0.001));
        });
    }

    @Test
    void nearest_normalisesVectors_soLengthDoesNotMatter() {
        // Given entry A = (2, 0, 0) and query (10, 0, 0)
        var index = OperationIndex.create(IndexKind.FLAT, entries("A", new float[] {2, 0, 0}), 0.5);

        // When searched
        var match = index.nearest(new float[] {10, 0, 0});

        // Then A is returned with a score of 1.0
        assertThat(match).hasValueSatisfying(m -> assertThat(m.score()).isCloseTo(1.0, within(1e-6)));
    }

    @Test
    void nearest_belowThreshold_returnsNoMatch() {
        // Given threshold 0.8 and entries far from the query (best score 0.3)
        var index = OperationIndex.create(IndexKind.FLAT, entries(
                "A", new float[] {0.3f, (float) Math.sqrt(1 - 0.09), 0}), 0.8);

        // When searched
        var match = index.nearest(new float[] {1, 0, 0});

        // Then no match is returned (empty), so the CLI can say it did not understand
        assertThat(match).isEmpty();
    }

    @Test
    void build_withDifferentDimensions_isRejected() {
        // Given one entry of 3 dimensions and one of 4
        // When the index is built
        // Then it fails naming the dimension mismatch
        assertThatThrownBy(() -> OperationIndex.create(IndexKind.FLAT,
                entries("A", new float[] {1, 0, 0}, "B", new float[] {1, 0, 0, 0}), 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimensions");
    }

    @Test
    void indexKindIvfPq_isDocumentedButNotBuilt() {
        // Given IndexKind.IVF_PQ
        // When an index of that kind is created
        // Then an UnsupportedOperationException explains it needs training data (thousands of
        // vectors) and that FLAT is exact for a catalog of this size
        assertThatThrownBy(() -> OperationIndex.create(IndexKind.IVF_PQ, entries("A", new float[] {1}), 0.5))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("training data")
                .hasMessageContaining("thousands")
                .hasMessageContaining("FLAT is exact");
    }

    @Test
    void justBelowThreshold_withClearWinner_matches() {
        // Given rule threshold 0.80, lower threshold 0.77, margin 0.015, and a query scoring 0.79
        // for A and 0.766 for B
        var index = OperationIndex.create(IndexKind.FLAT, List.of(
                new OperationIndex.Entry("A", unitAt(0.79, 0)), new OperationIndex.Entry("B", unitAt(0.766, 1))),
                new MatchRule(0.80, 0.77, 0.015));

        // When searched
        var match = index.nearest(new float[] {1, 0, 0, 0});

        // Then A is returned: it is below the threshold but leads B clearly
        assertThat(match).map(Match::id).contains("A");
    }

    @Test
    void justBelowThreshold_withNearTie_doesNotMatch() {
        // Given the same rule and a query scoring 0.790 for A and 0.787 for B (as unrelated text does)
        var index = OperationIndex.create(IndexKind.FLAT, List.of(
                new OperationIndex.Entry("A", unitAt(0.790, 0)), new OperationIndex.Entry("B", unitAt(0.787, 1))),
                new MatchRule(0.80, 0.77, 0.015));

        // When searched
        // Then nothing is returned
        assertThat(index.nearest(new float[] {1, 0, 0, 0})).isEmpty();
    }

    @Test
    void severalEntriesForOneOperation_bestEntryCounts() {
        // Given operation A with entries scoring 0.5 and 0.95, and B scoring 0.9
        var index = OperationIndex.create(IndexKind.FLAT, List.of(
                new OperationIndex.Entry("A", unitAt(0.5, 0)), new OperationIndex.Entry("A", unitAt(0.95, 1)),
                new OperationIndex.Entry("B", unitAt(0.9, 2))), MatchRule.absolute(0.8));

        // When searched
        // Then A wins with 0.95
        assertThat(index.nearest(new float[] {1, 0, 0, 0})).hasValueSatisfying(m -> {
            assertThat(m.id()).isEqualTo("A");
            assertThat(m.score()).isCloseTo(0.95, within(1e-6));
        });
    }

    /** A unit vector whose cosine with (1, 0, 0, 0) is {@code score}; the rest lies on axis 1 + side. */
    private static float[] unitAt(double score, int side) {
        float[] v = new float[4];
        v[0] = (float) score;
        v[1 + side] = (float) Math.sqrt(1 - score * score);
        return v;
    }
}
