package se.arbetsprov.ai.chat;

import java.util.ArrayList;
import java.util.List;
import se.arbetsprov.ai.catalog.RestCall;
import se.arbetsprov.ai.service.ServiceClient.ServiceResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns the service's JSON into a few readable lines in Swedish or English. Fixed templates per
 * operation, so the answer always matches the data; a problem detail from the service is shown
 * as its "detail" text.
 */
final class ResponseRenderer {

    private final JsonMapper json = JsonMapper.builder().build();

    List<String> render(RestCall call, ServiceResponse response, Language l) {
        if (!response.isSuccess()) {
            return List.of(problem(response));
        }
        JsonNode body = response.body().isBlank() ? null : json.readTree(response.body());
        return switch (call.operation().id()) {
            case "countries.list" -> countries(body, l);
            case "countries.get" -> List.of(Messages.country(l, text(body, "name"), text(body, "officialName"),
                    text(body, "capital"), text(body, "region"), text(body, "subregion"), body.path("population").asLong()));
            case "visits.list" -> visits(body, l);
            case "visits.create" -> List.of(Messages.visitCreated(l, text(body, "countryName")));
            case "visits.update" -> List.of(Messages.visitUpdated(l, text(body, "countryName")));
            case "visits.delete" -> List.of(Messages.visitDeleted(l, lastSegment(call.target())));
            default -> List.of(response.body());
        };
    }

    private List<String> countries(JsonNode page, Language l) {
        if (page.path("items").isEmpty()) {
            return List.of(Messages.noCountries(l));
        }
        var lines = new ArrayList<String>();
        int first = page.path("page").asInt() * page.path("size").asInt();
        int i = 0;
        for (JsonNode c : page.path("items")) {
            lines.add(Messages.countryLine(l, first + ++i, text(c, "name"), c.path("population").asLong()));
        }
        lines.add(Messages.pageFooter(l, page.path("page").asInt(), page.path("totalPages").asInt(),
                page.path("totalItems").asLong()));
        return lines;
    }

    private List<String> visits(JsonNode list, Language l) {
        if (list.isEmpty()) {
            return List.of(Messages.noVisits(l));
        }
        var lines = new ArrayList<String>();
        int i = 0;
        for (JsonNode v : list) {
            lines.add(Messages.visitLine(l, ++i, text(v, "countryName"), text(v, "countryCode"), text(v, "note")));
        }
        return lines;
    }

    private String problem(ServiceResponse response) {
        try {
            String detail = text(json.readTree(response.body()), "detail");
            return detail != null ? detail : "HTTP " + response.status();
        } catch (RuntimeException e) {
            return "HTTP " + response.status();
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static String lastSegment(String target) {
        return target.substring(target.lastIndexOf('/') + 1);
    }
}
