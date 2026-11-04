package se.arbetsprov.core.serialization;

import java.util.List;
import se.arbetsprov.core.exception.DeserializationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns JSON response bodies into typed values. Wraps Jackson so that no Jackson type appears in
 * the public API of the pipeline. Thread-safe.
 */
public final class JsonSerializer {

    private final JsonMapper mapper = JsonMapper.builder()
            // A cache keeps only the fields it needs; remote services may add fields at any time.
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /** Reads a single value; null for an empty body. */
    public <T> T deserialize(byte[] body, Class<T> type) {
        if (body.length == 0) {
            return null;
        }
        try {
            return mapper.readValue(body, type);
        } catch (JacksonException e) {
            throw new DeserializationException(type, e);
        }
    }

    /** Reads a JSON array; an empty list for an empty body. */
    public <T> List<T> deserializeList(byte[] body, Class<T> elementType) {
        if (body.length == 0) {
            return List.of();
        }
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, elementType);
        try {
            return List.copyOf(mapper.<List<T>>readValue(body, listType));
        } catch (JacksonException e) {
            throw new DeserializationException(elementType, e);
        }
    }
}
