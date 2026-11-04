package se.arbetsprov.service.testing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** A small HTTP client for calling the running app in tests. */
public final class Api {

    /** A response: status, content type, Location header and body parsed as JSON (null when empty or not JSON). */
    public record Result(int status, String contentType, String location, String text, JsonNode json) {
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newHttpClient();
    private final String base;

    public Api(int port) {
        this.base = "http://localhost:" + port;
    }

    public Result get(String path) {
        return send("GET", path, null, null);
    }

    public Result get(String path, String userId) {
        return send("GET", path, userId, null);
    }

    public Result post(String path, String userId, String json) {
        return send("POST", path, userId, json);
    }

    public Result put(String path, String userId, String json) {
        return send("PUT", path, userId, json);
    }

    public Result delete(String path, String userId) {
        return send("DELETE", path, userId, null);
    }

    private Result send(String method, String path, String userId, String json) {
        var builder = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, json == null ? BodyPublishers.noBody() : BodyPublishers.ofString(json));
        if (json != null) {
            builder.header("Content-Type", "application/json");
        }
        if (userId != null) {
            builder.header("X-User-Id", userId);
        }
        try {
            var response = http.send(builder.build(), BodyHandlers.ofString());
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            String text = response.body();
            JsonNode node = contentType.contains("json") && !text.isEmpty() ? JSON.readTree(text) : null;
            return new Result(response.statusCode(), contentType,
                    response.headers().firstValue("Location").orElse(null), text, node);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
