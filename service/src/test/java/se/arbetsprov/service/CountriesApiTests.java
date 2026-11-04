package se.arbetsprov.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import se.arbetsprov.service.testing.Api;
import tools.jackson.databind.JsonNode;

/**
 * User story 2: list countries over REST. Runs the whole app on the bundled snapshot
 * (no API key), so the data is real and fixed.
 */
@DisplayName("Countries API")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "countries.api-key=")
class CountriesApiTests {

    @LocalServerPort
    int port;

    private Api api() {
        return new Api(port);
    }

    private static List<String> names(JsonNode page) {
        var names = new ArrayList<String>();
        page.path("items").forEach(item -> names.add(item.path("name").asString()));
        return names;
    }

    @Test
    void list_withDefaults_returnsFirst20ByNameWithPaging() {
        // Given the app with the snapshot loaded
        // When GET /api/countries
        var result = api().get("/api/countries");

        // Then 200 with 20 items starting "Afghanistan", page 0, size 20, totalItems 250, totalPages 13
        assertThat(result.status()).isEqualTo(200);
        assertThat(names(result.json())).hasSize(20).first().isEqualTo("Afghanistan");
        assertThat(result.json().path("page").asInt()).isZero();
        assertThat(result.json().path("size").asInt()).isEqualTo(20);
        assertThat(result.json().path("totalItems").asLong()).isEqualTo(250);
        assertThat(result.json().path("totalPages").asInt()).isEqualTo(13);
    }

    @Test
    void list_europeByPopulationDescending_returnsMostPopulousFirst() {
        // Given the app with the snapshot loaded
        // When GET /api/countries?region=europe&sort=population&direction=desc&size=3
        var result = api().get("/api/countries?region=europe&sort=population&direction=desc&size=3");

        // Then the items are Russia, Germany, France (parameter values ignore letter case)
        assertThat(names(result.json())).containsExactly("Russia", "Germany", "France");
    }

    @Test
    void list_secondPage_returnsNextSlice() {
        // Given the app with the snapshot loaded
        var firstTen = names(api().get("/api/countries?size=10").json());

        // When GET /api/countries?page=1&size=5
        var result = api().get("/api/countries?page=1&size=5");

        // Then 5 items, the 6th to 10th by name, page 1
        assertThat(names(result.json())).containsExactlyElementsOf(firstTen.subList(5, 10));
        assertThat(result.json().path("page").asInt()).isEqualTo(1);
    }

    @Test
    void list_invalidParameters_return400ProblemNamingParameter() {
        // Given the app with the snapshot loaded
        // When GET /api/countries with size=0, then page=-1, then sort=area
        var badSize = api().get("/api/countries?size=0");
        var badPage = api().get("/api/countries?page=-1");
        var badSort = api().get("/api/countries?sort=area");

        // Then each answers 400 as application/problem+json whose detail names size, page or sort
        assertThat(List.of(badSize, badPage, badSort)).allSatisfy(r -> {
            assertThat(r.status()).isEqualTo(400);
            assertThat(r.contentType()).startsWith("application/problem+json");
        });
        assertThat(badSize.json().path("detail").asString()).contains("size");
        assertThat(badPage.json().path("detail").asString()).contains("page");
        assertThat(badSort.json().path("detail").asString()).contains("sort");
    }

    @Test
    void get_byCode_returnsCountry() {
        // Given the app with the snapshot loaded
        // When GET /api/countries/swe
        var country = api().get("/api/countries/swe").json();

        // Then 200 with code SWE, name Sweden, region Europe, capital Stockholm and a population
        assertThat(country.path("code").asString()).isEqualTo("SWE");
        assertThat(country.path("name").asString()).isEqualTo("Sweden");
        assertThat(country.path("region").asString()).isEqualTo("Europe");
        assertThat(country.path("capital").asString()).isEqualTo("Stockholm");
        assertThat(country.path("population").asLong()).isPositive();
    }

    @Test
    void get_unknownCode_returns404Problem() {
        // Given the app with the snapshot loaded
        // When GET /api/countries/XYZ
        var result = api().get("/api/countries/XYZ");

        // Then 404 as a problem detail naming XYZ
        assertThat(result.status()).isEqualTo(404);
        assertThat(result.contentType()).startsWith("application/problem+json");
        assertThat(result.json().path("detail").asString()).contains("XYZ");
    }
}
