package se.arbetsprov.ai.cli;

import java.nio.file.Path;
import java.util.List;
import se.arbetsprov.ai.embedding.E5EmbeddingModel;
import se.arbetsprov.ai.embedding.EmbeddingModel;
import se.arbetsprov.ai.llm.OllamaParameterFiller;

/**
 * Everything the CLI touches outside itself. The real one uses the user's cache folder, real
 * processes and the real model; tests pass temporary folders, fakes and stubs.
 *
 * @param cacheDir    where models and the setup marker live
 * @param os          the operating system, for installing Ollama
 * @param commands    runs ollama, winget and brew
 * @param files       the model files to set up
 * @param ollamaModel the Ollama model to set up
 * @param embeddings  creates the embedding model from the cached files
 */
public record AskContext(
        Path cacheDir,
        Os os,
        CommandRunner commands,
        List<ModelFile> files,
        String ollamaModel,
        EmbeddingFactory embeddings) {

    @FunctionalInterface
    public interface EmbeddingFactory {
        EmbeddingModel create(Path modelFile, Path tokenizerFile);
    }

    public AskContext {
        files = List.copyOf(files);
    }

    public static AskContext defaults() {
        return new AskContext(
                Path.of(System.getProperty("user.home"), ".cache", "arbetsprov"),
                Os.current(),
                CommandRunner.system(),
                ModelFile.defaults(),
                OllamaParameterFiller.DEFAULT_MODEL,
                E5EmbeddingModel::new);
    }

    public Path modelsDir() {
        return cacheDir.resolve("models");
    }

    public Path markerFile() {
        return cacheDir.resolve("setup.json");
    }
}
