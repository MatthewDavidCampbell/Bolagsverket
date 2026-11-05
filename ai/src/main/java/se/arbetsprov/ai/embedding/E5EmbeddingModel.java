package se.arbetsprov.ai.embedding;

import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import java.nio.file.Path;
import se.arbetsprov.ai.AskException;

/**
 * multilingual-e5-small run in-process with ONNX Runtime. A Swedish question and its English
 * catalog entry land close together, so the catalog only has to be written in English.
 *
 * <p>e5 models are trained with the prefixes {@code query: } and {@code passage: } and mean
 * pooling; leaving them out lowers the quality of the match.
 */
public final class E5EmbeddingModel implements EmbeddingModel {

    private final OnnxEmbeddingModel model;

    public E5EmbeddingModel(Path modelFile, Path tokenizerFile) {
        try {
            this.model = new OnnxEmbeddingModel(modelFile, tokenizerFile, PoolingMode.MEAN);
        } catch (RuntimeException e) {
            throw new AskException(AskException.MISSING_PREREQUISITE,
                    "Kunde inte läsa modellen " + modelFile.getFileName() + ". Kör: ask --setup", e);
        }
    }

    @Override
    public float[] embedQuery(String text) {
        return model.embed("query: " + text).content().vector();
    }

    @Override
    public float[] embedPassage(String text) {
        return model.embed("passage: " + text).content().vector();
    }
}
