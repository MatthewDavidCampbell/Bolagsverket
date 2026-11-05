package se.arbetsprov.ai.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * User story 3: the canonical English catalog, and requests built only from its templates.
 */
@DisplayName("Operation catalog")
class OperationCatalogTests {

    private final OperationCatalog catalog = OperationCatalog.standard();

    private Operation op(String id) {
        return catalog.byId(id).orElseThrow();
    }

    @Test
    void catalog_hasTheSixOperationsInEnglish() {
        // Given the catalog
        // When its operations are listed
        // Then they are List countries, Get country, List my visits, Mark country as visited,
        // Change visit note and Unmark visited country, each with an English description
        assertThat(catalog.operations()).extracting(Operation::instruction).containsExactly(
                "List countries", "Get country", "List my visits", "Mark country as visited",
                "Change visit note", "Unmark visited country");
        assertThat(catalog.operations()).allSatisfy(o -> assertThat(o.description()).isNotBlank().endsWith("."));
    }

    @Test
    void listCountries_buildsQueryFromValidatedValues() {
        // Given List countries with region "Europe", sort "population", direction "desc", size 5
        var values = Map.<String, Object>of("region", "europe", "sort", "population", "direction", "DESC", "size", 5);

        // When the request is built
        var call = catalog.build(op("countries.list"), values, "alice");

        // Then it is GET /api/countries?region=Europe&sort=population&direction=desc&size=5
        assertThat(call.display()).isEqualTo("GET /api/countries?region=Europe&sort=population&direction=desc&size=5");
        assertThat(call.headers()).isEmpty();
    }

    @Test
    void listCountries_omitsParametersThatAreNotGiven() {
        // Given List countries with no values
        // When the request is built
        var call = catalog.build(op("countries.list"), Map.of(), "alice");

        // Then it is GET /api/countries with no query string
        assertThat(call.display()).isEqualTo("GET /api/countries");
    }

    @Test
    void valueOutsideSchema_isRejectedBeforeAnyCall() {
        // Given sort "area", and separately region "Atlantis" and size 500
        // When the request is built
        // Then each is rejected naming the parameter and its allowed values or range
        assertThatThrownBy(() -> catalog.build(op("countries.list"), Map.of("sort", "area"), "alice"))
                .isInstanceOf(InvalidParametersException.class).hasMessageContaining("sort must be one of name, population");
        assertThatThrownBy(() -> catalog.build(op("countries.list"), Map.of("region", "Atlantis"), "alice"))
                .isInstanceOf(InvalidParametersException.class).hasMessageContaining("region must be one of");
        assertThatThrownBy(() -> catalog.build(op("countries.list"), Map.of("size", 500), "alice"))
                .isInstanceOf(InvalidParametersException.class).hasMessageContaining("size must be between 1 and 100");
    }

    @Test
    void markVisited_buildsPostWithJsonBodyAndUserHeader() {
        // Given Mark country as visited with countryCode "swe", note "midsommar", user "alice"
        var call = catalog.build(op("visits.create"), Map.of("countryCode", "swe", "note", "midsommar"), "alice");

        // When the request is built
        // Then it is POST /api/visits with body {"countryCode":"SWE","note":"midsommar"}
        // and header X-User-Id: alice
        assertThat(call.method()).isEqualTo("POST");
        assertThat(call.target()).isEqualTo("/api/visits");
        assertThat(new String(call.body(), StandardCharsets.UTF_8)).isEqualTo("{\"countryCode\":\"SWE\",\"note\":\"midsommar\"}");
        assertThat(call.headers()).containsEntry("X-User-Id", "alice");
    }

    @Test
    void pathParameters_areEncodedAndCannotChangeThePath() {
        // Given Get country with country "../visits"
        // When the request is built
        // Then it is rejected (codes are three letters), so no other path can be reached
        assertThatThrownBy(() -> catalog.build(op("countries.get"), Map.of("countryCode", "../visits"), "alice"))
                .isInstanceOf(InvalidParametersException.class).hasMessageContaining("countryCode");
        assertThat(catalog.build(op("countries.get"), Map.of("countryCode", "nor"), "alice").target())
                .isEqualTo("/api/countries/NOR");
    }

    @Test
    void unknownParameterName_isRejected() {
        // Given List countries with an extra value "url"
        // When the request is built
        // Then it is rejected naming "url"
        assertThatThrownBy(() -> catalog.build(op("countries.list"), Map.of("url", "http://evil.test"), "alice"))
                .isInstanceOf(InvalidParametersException.class).hasMessageContaining("url");
    }
}
