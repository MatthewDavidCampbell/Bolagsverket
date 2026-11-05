package se.arbetsprov.ai.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.ai.catalog.OperationCatalog;
import se.arbetsprov.ai.chat.Assistant;
import se.arbetsprov.ai.cli.AskContext;
import se.arbetsprov.ai.cli.ModelFile;
import se.arbetsprov.ai.index.IndexKind;
import se.arbetsprov.ai.index.Match;
import se.arbetsprov.ai.index.MatchRule;
import se.arbetsprov.ai.index.OperationIndex;

/**
 * User story 2 with the real multilingual-e5-small. Runs only when the model is in the cache
 * folder (after --setup); skipped otherwise, so CI does not need it.
 */
@DisplayName("Real embedding model")
class RealModelTests {

    private static EmbeddingModel model;
    private static OperationIndex index;

    @BeforeAll
    static void load() {
        Path dir = AskContext.defaults().modelsDir();
        Path modelFile = dir.resolve(ModelFile.MODEL);
        Path tokenizer = dir.resolve(ModelFile.TOKENIZER);
        assumeTrue(Files.isRegularFile(modelFile) && Files.isRegularFile(tokenizer),
                "multilingual-e5-small not in " + dir + "; run ask --setup to include this test");
        model = new E5EmbeddingModel(modelFile, tokenizer);
        index = OperationIndex.create(IndexKind.FLAT, Assistant.entries(OperationCatalog.standard(), model),
                MatchRule.DEFAULT);
    }

    @Test
    void swedishAndEnglishQuestions_matchTheRightOperation() {
        // Given the catalog embedded with the real model
        var expected = new LinkedHashMap<String, String>();
        expected.put("lista de fem största länderna i Europa", "countries.list");
        expected.put("show the largest countries in Asia", "countries.list");
        expected.put("vad är huvudstaden i Norge?", "countries.get");
        expected.put("vilka länder har jag besökt?", "visits.list");
        expected.put("markera Sverige som besökt", "visits.create");
        expected.put("ändra min anteckning för Japan", "visits.update");
        expected.put("ta bort Finland från mina besök", "visits.delete");
        // Not phrased like any catalog example:
        expected.put("hur många bor i Danmark?", "countries.get");
        expected.put("jag har varit i Italien", "visits.create");
        // Just below the threshold, but a clear winner (score-gap rule):
        expected.put("vilka är de tre minsta länderna i Oceanien?", "countries.list");

        // When each question is matched
        var actual = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> e : expected.entrySet()) {
            actual.put(e.getKey(), index.nearest(model.embedQuery(e.getKey())).map(Match::id).orElse("no match"));
        }

        // Then each lands on the expected operation above the threshold
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void unrelatedQuestion_staysBelowThreshold() {
        // Given the real model
        // When "vad blir vädret i morgon?" is matched
        // (and a request outside the service, "skriv en dikt om hösten")
        // Then no operation is returned
        assertThat(index.nearest(model.embedQuery("vad blir vädret i morgon?"))).isEmpty();
        assertThat(index.nearest(model.embedQuery("skriv en dikt om hösten"))).isEmpty();
    }
}
