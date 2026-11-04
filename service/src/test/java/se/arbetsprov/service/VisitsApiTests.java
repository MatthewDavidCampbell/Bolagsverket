package se.arbetsprov.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import se.arbetsprov.service.testing.Api;

/**
 * User story 3: manage visited countries over REST. Every call carries X-User-Id.
 * Each test uses its own user id, so tests do not share visits.
 */
@DisplayName("Visits API")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "countries.api-key=")
class VisitsApiTests {

    @LocalServerPort
    int port;

    private final String alice = "alice-" + UUID.randomUUID();
    private final String bob = "bob-" + UUID.randomUUID();

    private Api api() {
        return new Api(port);
    }

    private static void assertProblem(Api.Result result, int status) {
        assertThat(result.status()).isEqualTo(status);
        assertThat(result.contentType()).startsWith("application/problem+json");
    }

    @Test
    void create_returns201WithLocationAndCountryName() {
        // Given user "alice"
        // When POST /api/visits {"countryCode":"swe","note":"Midsummer"}
        var result = api().post("/api/visits", alice, "{\"countryCode\":\"swe\",\"note\":\"Midsummer\"}");

        // Then 201, Location ends with /api/visits/SWE, and the body has countryCode SWE,
        // countryName Sweden, note Midsummer, createdAt and updatedAt
        assertThat(result.status()).isEqualTo(201);
        assertThat(result.location()).endsWith("/api/visits/SWE");
        assertThat(result.json().path("countryCode").asString()).isEqualTo("SWE");
        assertThat(result.json().path("countryName").asString()).isEqualTo("Sweden");
        assertThat(result.json().path("note").asString()).isEqualTo("Midsummer");
        assertThat(result.json().path("createdAt").asString()).isNotBlank();
        assertThat(result.json().path("updatedAt").asString()).isNotBlank();
    }

    @Test
    void create_twice_returns409Problem() {
        // Given "alice" has visited SWE
        api().post("/api/visits", alice, "{\"countryCode\":\"SWE\"}");

        // When POST /api/visits {"countryCode":"SWE"} again
        var result = api().post("/api/visits", alice, "{\"countryCode\":\"SWE\"}");

        // Then 409 as a problem detail
        assertProblem(result, 409);
    }

    @Test
    void create_unknownCountry_returns400Problem() {
        // Given user "alice"
        // When POST /api/visits {"countryCode":"XYZ"}
        var result = api().post("/api/visits", alice, "{\"countryCode\":\"XYZ\"}");

        // Then 400 as a problem detail naming XYZ
        assertProblem(result, 400);
        assertThat(result.json().path("detail").asString()).contains("XYZ");
    }

    @Test
    void create_noteOver500Characters_returns400Problem() {
        // Given user "alice"
        // When POST /api/visits with a 501-character note
        var result = api().post("/api/visits", alice,
                "{\"countryCode\":\"SWE\",\"note\":\"" + "a".repeat(501) + "\"}");

        // Then 400 as a problem detail naming note
        assertProblem(result, 400);
        assertThat(result.json().path("detail").asString()).contains("note");
    }

    @Test
    void anyVisitsCall_withoutUserHeader_returns400Problem() {
        // Given no X-User-Id header
        // When GET /api/visits
        var result = api().get("/api/visits");

        // Then 400 as a problem detail naming X-User-Id
        assertProblem(result, 400);
        assertThat(result.json().path("detail").asString()).contains("X-User-Id");
    }

    @Test
    void list_returnsOnlyOwnVisitsWithNamesAndNotes() {
        // Given "alice" visited SWE (note "Fika") and NOR, and "bob" visited JPN
        api().post("/api/visits", alice, "{\"countryCode\":\"SWE\",\"note\":\"Fika\"}");
        api().post("/api/visits", alice, "{\"countryCode\":\"NOR\"}");
        api().post("/api/visits", bob, "{\"countryCode\":\"JPN\"}");

        // When "alice" calls GET /api/visits
        var result = api().get("/api/visits", alice);

        // Then NOR then SWE are returned with countryName and note; JPN is not
        var rows = new ArrayList<String>();
        result.json().forEach(v -> rows.add(v.path("countryCode").asString() + "|"
                + v.path("countryName").asString() + "|" + v.path("note").asString("")));
        assertThat(rows).containsExactly("NOR|Norway|", "SWE|Sweden|Fika");
    }

    @Test
    void get_visited_returnsVisit_andNotVisited_returns404() {
        // Given "alice" visited SWE
        api().post("/api/visits", alice, "{\"countryCode\":\"SWE\"}");

        // When GET /api/visits/SWE and GET /api/visits/FIN
        var visited = api().get("/api/visits/SWE", alice);
        var notVisited = api().get("/api/visits/FIN", alice);

        // Then 200 with the SWE visit, and 404 for FIN
        assertThat(visited.status()).isEqualTo(200);
        assertThat(visited.json().path("countryCode").asString()).isEqualTo("SWE");
        assertProblem(notVisited, 404);
    }

    @Test
    void updateNote_visited_returns200WithNewNote() {
        // Given "alice" visited SWE with note "old"
        api().post("/api/visits", alice, "{\"countryCode\":\"SWE\",\"note\":\"old\"}");

        // When PUT /api/visits/SWE {"note":"Stockholm"}
        var result = api().put("/api/visits/SWE", alice, "{\"note\":\"Stockholm\"}");

        // Then 200 with note Stockholm, and a later GET shows the same
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.json().path("note").asString()).isEqualTo("Stockholm");
        assertThat(api().get("/api/visits/SWE", alice).json().path("note").asString()).isEqualTo("Stockholm");
    }

    @Test
    void updateNote_notVisited_returns404() {
        // Given "alice" has not visited FIN
        // When PUT /api/visits/FIN {"note":"x"}
        var result = api().put("/api/visits/FIN", alice, "{\"note\":\"x\"}");

        // Then 404 as a problem detail
        assertProblem(result, 404);
    }

    @Test
    void delete_visited_returns204_thenSecondDeleteReturns404() {
        // Given "alice" visited SWE
        api().post("/api/visits", alice, "{\"countryCode\":\"SWE\"}");

        // When DELETE /api/visits/SWE twice
        var first = api().delete("/api/visits/SWE", alice);
        var second = api().delete("/api/visits/SWE", alice);

        // Then 204, then 404, and SWE is no longer listed
        assertThat(first.status()).isEqualTo(204);
        assertProblem(second, 404);
        assertThat(api().get("/api/visits", alice).json().size()).isZero();
    }
}
