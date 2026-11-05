package se.arbetsprov.ai.catalog;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A request to the service, built only from a catalog operation.
 *
 * @param operation the operation it was built from
 * @param method    HTTP method
 * @param target    path and query string, for example {@code /api/countries?size=5}
 * @param body      JSON body, or null
 * @param headers   extra headers, for example X-User-Id
 */
public record RestCall(Operation operation, String method, String target, byte[] body, Map<String, String> headers) {

    public RestCall {
        headers = Map.copyOf(headers);
    }

    /** One line for the chat, for example {@code POST /api/visits {"countryCode":"SWE"}}. */
    public String display() {
        return body == null ? method + " " + target : method + " " + target + " " + new String(body, StandardCharsets.UTF_8);
    }
}
