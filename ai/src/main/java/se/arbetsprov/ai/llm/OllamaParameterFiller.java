package se.arbetsprov.ai.llm;

import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import se.arbetsprov.ai.AskException;
import se.arbetsprov.ai.catalog.Operation;
import se.arbetsprov.ai.catalog.ParameterSpec;
import se.arbetsprov.core.client.ClientOptions;
import se.arbetsprov.core.exception.DeserializationException;
import se.arbetsprov.core.http.HttpHeaders;
import se.arbetsprov.core.http.HttpMethod;
import se.arbetsprov.core.http.HttpPipeline;
import se.arbetsprov.core.http.HttpRequest;
import se.arbetsprov.core.serialization.JsonSerializer;

/**
 * Asks a small local model through Ollama's REST API to fill one operation's parameters. The
 * model gets the English instruction and the parameter schema, and must answer with JSON that
 * follows the schema (Ollama's {@code format}); temperature 0 keeps answers repeatable. It calls
 * Ollama through the core pipeline, like any other remote service.
 */
public final class OllamaParameterFiller implements ParameterFiller {

    /** The default model: small enough for a laptop, good enough for filling a few fields. */
    public static final String DEFAULT_MODEL = "qwen2.5:1.5b";

    private final HttpPipeline pipeline;
    private final URI baseUri;
    private final String model;
    private final JsonSerializer json = new JsonSerializer();

    public OllamaParameterFiller(URI baseUri, String model) {
        this.baseUri = baseUri;
        this.model = model;
        this.pipeline = new ClientOptions().baseUri(baseUri).buildPipeline();
    }

    @Override
    public Filled fill(Operation operation, String question) {
        var body = Map.of(
                "model", model,
                "stream", false,
                "options", Map.of("temperature", 0),
                "format", schema(operation),
                "messages", List.of(
                        Map.of("role", "system", "content", instruction(operation)),
                        Map.of("role", "user", "content", question)));
        var request = new HttpRequest(HttpMethod.POST, chatUri(),
                HttpHeaders.empty().with("Content-Type", "application/json"), json.serialize(body));

        var response = send(request);
        if (response.status() == 404 && response.bodyAsString().contains("not found")) {
            throw new AskException(AskException.MISSING_PREREQUISITE,
                    "Ollama-modellen " + model + " saknas. Kör först: ask --setup");
        }
        if (!response.isSuccess()) {
            throw new AskException(AskException.UNREACHABLE,
                    "Ollama på " + baseUri + " svarade " + response.status() + ": " + response.bodyAsString());
        }
        return read(response.body());
    }

    private se.arbetsprov.core.http.HttpResponse send(HttpRequest request) {
        try {
            return pipeline.send(request);
        } catch (UncheckedIOException e) {
            throw new AskException(AskException.UNREACHABLE,
                    "Ollama svarar inte på " + baseUri + ". Starta Ollama och försök igen.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Filled read(byte[] responseBody) {
        try {
            var reply = json.deserialize(responseBody, Map.class);
            var message = reply == null ? null : (Map<String, Object>) reply.get("message");
            var content = message == null ? null : (String) message.get("content");
            if (content == null) {
                return Filled.notUnderstood();
            }
            var values = new HashMap<String, Object>(
                    json.deserialize(content.getBytes(java.nio.charset.StandardCharsets.UTF_8), Map.class));
            values.values().removeIf(v -> v == null);
            return Filled.of(values);
        } catch (DeserializationException | ClassCastException e) {
            return Filled.notUnderstood();
        }
    }

    private URI chatUri() {
        String base = baseUri.toString();
        return URI.create((base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/api/chat");
    }

    /** What the model is told: the operation, its parameters and how to answer. */
    static String instruction(Operation operation) {
        var text = new StringBuilder()
                .append("You fill in parameters for one REST API operation from the user's message.\n")
                .append("Operation: ").append(operation.instruction()).append('\n')
                .append("Description: ").append(operation.description()).append('\n')
                .append("Parameters:\n");
        for (ParameterSpec p : operation.parameters()) {
            text.append("- ").append(p.name()).append(": ").append(p.description());
            if (!p.allowed().isEmpty()) {
                text.append(" (one of ").append(String.join(", ", p.allowed())).append(')');
            }
            text.append(p.required() ? " (required)" : " (optional, leave out if not mentioned)").append('\n');
        }
        return text.append("Answer only with JSON.").toString();
    }

    /** A JSON schema of the parameters, so the model can only answer in that shape. */
    static Map<String, Object> schema(Operation operation) {
        var properties = new LinkedHashMap<String, Object>();
        var required = new ArrayList<String>();
        for (ParameterSpec p : operation.parameters()) {
            var property = new LinkedHashMap<String, Object>();
            property.put("type", p.type() == ParameterSpec.Type.NUMBER ? "integer" : "string");
            if (!p.allowed().isEmpty()) {
                property.put("enum", p.allowed());
            }
            properties.put(p.name(), property);
            if (p.required()) {
                required.add(p.name());
            }
        }
        return Map.of("type", "object", "properties", properties, "required", required);
    }
}
