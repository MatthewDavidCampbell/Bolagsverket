package se.arbetsprov.ai.catalog;

import static se.arbetsprov.ai.catalog.ParameterSpec.Location.BODY;
import static se.arbetsprov.ai.catalog.ParameterSpec.Location.PATH;
import static se.arbetsprov.ai.catalog.ParameterSpec.Location.QUERY;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import se.arbetsprov.core.serialization.JsonSerializer;

/**
 * Everything the chat can ask the service to do, in canonical English. Requests are built only
 * here, from an operation's method, path template and validated parameters, so the LLM can never
 * choose a URL.
 */
public final class OperationCatalog {

    private final List<Operation> operations;
    private final JsonSerializer json = new JsonSerializer();

    public OperationCatalog(List<Operation> operations) {
        this.operations = List.copyOf(operations);
    }

    /** The six operations of the countries and visits service. */
    public static OperationCatalog standard() {
        return new OperationCatalog(List.of(
                new Operation("countries.list", "List countries",
                        "List countries, optionally in one region, sorted by name or by population (size), "
                                + "ascending or descending, one page at a time.",
                        "GET", "/api/countries", List.of(
                        ParameterSpec.choice("region", QUERY, "Region in English",
                                "Africa", "Americas", "Antarctic", "Asia", "Europe", "Oceania"),
                        ParameterSpec.choice("sort", QUERY,
                                "population when the question is about size, people, largest, smallest, biggest or most; "
                                        + "name when it is alphabetical or sorting is not mentioned",
                                "name", "population"),
                        ParameterSpec.choice("direction", QUERY,
                                "desc for largest, biggest, most people or Z to A; asc for smallest, fewest people or A to Z",
                                "asc", "desc"),
                        ParameterSpec.number("page", QUERY, 0, 10_000, "Page number from 0"),
                        ParameterSpec.number("size", QUERY, 1, 100, "How many countries, for example 5 for top five")),
                        false, List.of(
                        "Show the largest countries in Europe",
                        "Countries sorted by population",
                        "The smallest countries in Africa",
                        "List all countries in Asia in alphabetical order",
                        "Top five countries by size")),
                new Operation("countries.get", "Get country",
                        "Show facts about one country: its capital, region and population.",
                        "GET", "/api/countries/{countryCode}", List.of(ParameterSpec.countryCode(PATH)), false,
                        List.of(
                        "What is the capital of Norway?",
                        "Tell me about Japan",
                        "How many people live in Sweden?",
                        "Facts about Germany")),
                new Operation("visits.list", "List my visits",
                        "Show the countries I have visited, with my notes.",
                        "GET", "/api/visits", List.of(), true,
                        List.of(
                        "Which countries have I visited?",
                        "Show my visited countries and notes",
                        "Where have I been?")),
                new Operation("visits.create", "Mark country as visited",
                        "Mark a country as visited, optionally with a short note about the visit.",
                        "POST", "/api/visits", List.of(
                        ParameterSpec.countryCode(BODY),
                        ParameterSpec.text("note", BODY, 500,
                                "Only a note the user explicitly gives, for example after note: or anteckning:; "
                                        + "leave out otherwise")),
                        true, List.of(
                        "Mark Sweden as visited",
                        "I have been to Japan, note: cherry blossoms",
                        "Add Norway to my visited countries",
                        "I visited Spain last summer")),
                new Operation("visits.update", "Change visit note",
                        "Change or remove the note on a country I have visited.",
                        "PUT", "/api/visits/{countryCode}", List.of(
                        ParameterSpec.countryCode(PATH),
                        ParameterSpec.text("note", BODY, 500, "The new note, in the user's words")),
                        true, List.of(
                        "Change my note for Japan",
                        "Update the note on one of my visits",
                        "Remove the note from my visit to France")),
                new Operation("visits.delete", "Unmark visited country",
                        "Remove a country from my visited countries.",
                        "DELETE", "/api/visits/{countryCode}", List.of(ParameterSpec.countryCode(PATH)), true,
                        List.of(
                        "Remove Finland from my visits",
                        "Unmark Spain as visited",
                        "I have not been to Chile after all"))));
    }

    public List<Operation> operations() {
        return operations;
    }

    public Optional<Operation> byId(String id) {
        return operations.stream().filter(o -> o.id().equals(id)).findFirst();
    }

    /**
     * Builds the request for {@code operation} from suggested values. Values that are null are
     * treated as not given.
     *
     * @throws InvalidParametersException for an unknown parameter, a missing required one or a value
     *                                    outside the schema
     */
    public RestCall build(Operation operation, Map<String, Object> values, String userId) {
        for (String name : values.keySet()) {
            if (operation.parameters().stream().noneMatch(p -> p.name().equals(name))) {
                throw new InvalidParametersException("Unknown parameter: " + name);
            }
        }

        String path = operation.pathTemplate();
        var query = new StringJoiner("&");
        var body = new LinkedHashMap<String, Object>();
        for (ParameterSpec spec : operation.parameters()) {
            Object raw = values.get(spec.name());
            if (raw == null || (raw instanceof String s && s.isBlank())) {
                if (spec.required()) {
                    throw new InvalidParametersException("Missing parameter: " + spec.name());
                }
                continue;
            }
            Object value = spec.validate(raw);
            switch (spec.location()) {
                case PATH -> path = path.replace("{" + spec.name() + "}", encode(value));
                case QUERY -> query.add(spec.name() + "=" + encode(value));
                case BODY -> body.put(spec.name(), value);
            }
        }

        String target = query.length() == 0 ? path : path + "?" + query;
        byte[] payload = operation.parameters().stream().anyMatch(p -> p.location() == BODY) ? json.serialize(body) : null;
        Map<String, String> headers = operation.needsUser() ? Map.of("X-User-Id", userId) : Map.of();
        return new RestCall(operation, operation.method(), target, payload, headers);
    }

    private static String encode(Object value) {
        return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
    }
}
