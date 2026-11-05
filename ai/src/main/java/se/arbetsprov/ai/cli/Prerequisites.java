package se.arbetsprov.ai.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import se.arbetsprov.ai.AskException;
import se.arbetsprov.core.client.ClientOptions;
import se.arbetsprov.core.exception.DeserializationException;
import se.arbetsprov.core.http.HttpRequest;
import se.arbetsprov.core.serialization.JsonSerializer;

/**
 * The one check both phases share. {@code ask --setup} uses it to decide what to install;
 * {@code ask --user} uses it to fail fast before the chat starts.
 *
 * <p>Model files are checked against the marker that setup writes after verifying their SHA-256,
 * by size and modification time, so starting the chat does not re-hash 135 MB. Ollama is always
 * checked live, because it can be stopped or removed at any time.
 */
public final class Prerequisites {

    /** What Ollama says about itself. */
    public enum OllamaStatus { READY, MODEL_MISSING, UNREACHABLE }

    private static final JsonSerializer JSON = new JsonSerializer();

    private Prerequisites() {
    }

    /**
     * Passes when everything is in place; otherwise fails with exit code 3 (missing or changed
     * files, missing Ollama model) or 4 (Ollama not answering).
     */
    public static void requireReady(AskContext context, URI ollama) {
        var marker = readMarker(context);
        var missing = new ArrayList<String>();
        var changed = new ArrayList<String>();
        for (ModelFile file : context.files()) {
            Path path = context.modelsDir().resolve(file.name());
            var recorded = marker.get(file.name());
            if (!Files.isRegularFile(path) || recorded == null) {
                missing.add(file.name());
            } else if (!matches(path, recorded)) {
                changed.add(file.name());
            }
        }

        var status = ollamaStatus(ollama, context.ollamaModel());
        if (status == OllamaStatus.MODEL_MISSING) {
            missing.add(context.ollamaModel());
        }
        if (!missing.isEmpty() || !changed.isEmpty()) {
            var parts = new ArrayList<String>();
            if (!missing.isEmpty()) {
                parts.add("Saknas: " + String.join(", ", missing));
            }
            if (!changed.isEmpty()) {
                parts.add("Ändrad sedan setup: " + String.join(", ", changed));
            }
            throw new AskException(AskException.MISSING_PREREQUISITE, String.join(". ", parts) + ". Kör först: ask --setup");
        }
        if (status == OllamaStatus.UNREACHABLE) {
            throw new AskException(AskException.UNREACHABLE,
                    "Ollama svarar inte på " + ollama + ". Starta Ollama och försök igen.");
        }
    }

    /** Asks Ollama which models it has ({@code GET /api/tags}). */
    @SuppressWarnings("unchecked")
    public static OllamaStatus ollamaStatus(URI ollama, String model) {
        try {
            var pipeline = new ClientOptions().baseUri(ollama).buildPipeline();
            String base = ollama.toString().replaceAll("/+$", "");
            var response = pipeline.send(HttpRequest.get(URI.create(base + "/api/tags")));
            if (!response.isSuccess()) {
                return OllamaStatus.UNREACHABLE;
            }
            var tags = JSON.deserialize(response.body(), Map.class);
            List<Map<String, Object>> models = tags == null
                    ? List.of() : (List<Map<String, Object>>) tags.getOrDefault("models", List.of());
            return models.stream().anyMatch(m -> model.equals(m.get("name")))
                    ? OllamaStatus.READY : OllamaStatus.MODEL_MISSING;
        } catch (UncheckedIOException | DeserializationException | ClassCastException e) {
            return OllamaStatus.UNREACHABLE;
        }
    }

    /** File name to {sha256, size, modified} as recorded by the last setup. */
    @SuppressWarnings("unchecked")
    static Map<String, Map<String, Object>> readMarker(AskContext context) {
        try {
            if (!Files.isRegularFile(context.markerFile())) {
                return Map.of();
            }
            var marker = JSON.deserialize(Files.readAllBytes(context.markerFile()), Map.class);
            return marker == null ? Map.of() : (Map<String, Map<String, Object>>) marker.getOrDefault("files", Map.of());
        } catch (IOException | DeserializationException | ClassCastException e) {
            return Map.of();
        }
    }

    /** Records verified files so later chats can trust them by size and modification time. */
    static void writeMarker(AskContext context, Map<String, String> verifiedSha256) {
        var files = new LinkedHashMap<String, Object>();
        for (var e : verifiedSha256.entrySet()) {
            Path path = context.modelsDir().resolve(e.getKey());
            files.put(e.getKey(), Map.of("sha256", e.getValue(), "size", size(path), "modified", modified(path)));
        }
        try {
            Files.createDirectories(context.cacheDir());
            Files.write(context.markerFile(), JSON.serialize(Map.of("files", files)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean matches(Path path, Map<String, Object> recorded) {
        return recorded.get("size") instanceof Number s && s.longValue() == size(path)
                && recorded.get("modified") instanceof Number m && m.longValue() == modified(path);
    }

    private static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1;
        }
    }

    private static long modified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return -1;
        }
    }
}
