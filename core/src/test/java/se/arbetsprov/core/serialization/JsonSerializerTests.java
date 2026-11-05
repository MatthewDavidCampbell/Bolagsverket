package se.arbetsprov.core.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Writing request bodies, added for the ai client (Ollama and POST/PUT to the service).
 */
@DisplayName("JSON serializer")
class JsonSerializerTests {

    record Sample(String name, long population, List<String> capitals) {
    }

    private final JsonSerializer serializer = new JsonSerializer();

    @Test
    void serialize_record_writesJsonThatReadsBackEqual() {
        // Given a record with a string, a number and a list
        var sample = new Sample("Bolivia", 12_000_000, List.of("Sucre", "La Paz"));

        // When it is serialized and deserialized again
        var copy = serializer.deserialize(serializer.serialize(sample), Sample.class);

        // Then the result equals the original
        assertThat(copy).isEqualTo(sample);
    }

    @Test
    void serialize_map_writesUtf8() {
        // Given the map {"note": "midsommar på Åland"}
        // When serialized
        var bytes = serializer.serialize(Map.of("note", "midsommar på Åland"));

        // Then the bytes are UTF-8 JSON containing "midsommar på Åland"
        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("{\"note\":\"midsommar på Åland\"}");
    }
}
