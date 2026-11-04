package se.arbetsprov.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import se.arbetsprov.service.testing.Api;

/**
 * User stories 1 and 4: the app starts with countries loaded, reports ready, and documents
 * every endpoint in Swagger. The whole app runs in-process; REST Countries is never called.
 * The live-import case needs its own configuration and lives in {@link LiveImportTests}.
 */
@DisplayName("Startup and documentation")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "countries.api-key=")
@Import(StartupTests.ThreadProbe.class)
class StartupTests {

    /** Test-only endpoint that reports whether the request ran on a virtual thread. */
    @TestConfiguration
    @RestController
    static class ThreadProbe {

        @GetMapping("/test/thread")
        String thread() {
            return Thread.currentThread().isVirtual() ? "virtual" : "platform";
        }
    }

    @LocalServerPort
    int port;

    private Api api() {
        return new Api(port);
    }

    @Test
    void startup_withoutKey_isReadyWith250CountriesFromSnapshot() {
        // Given the app started with no API key
        // When GET /api/countries?size=1 is called
        var result = api().get("/api/countries?size=1");

        // Then totalItems is 250
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.json().path("totalItems").asLong()).isEqualTo(250);
    }

    @Test
    void readinessProbe_afterStart_isUp() {
        // Given the app has started
        // When GET /actuator/health/readiness is called
        var result = api().get("/actuator/health/readiness");

        // Then 200 with status "UP"
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.json().path("status").asString()).isEqualTo("UP");
    }

    @Test
    void requests_runOnVirtualThreads() {
        // Given the app has started with spring.threads.virtual.enabled
        // When a request is handled
        var result = api().get("/test/thread");

        // Then the handling thread is virtual (checked through a test-only endpoint)
        assertThat(result.text()).isEqualTo("virtual");
    }

    @Test
    void openApiDocument_listsEveryEndpointWithUserHeader() {
        // Given the app has started
        // When GET /v3/api-docs is called
        var paths = api().get("/v3/api-docs").json().path("paths");

        // Then it lists /api/countries, /api/countries/{code}, /api/visits and /api/visits/{code}
        // with their methods, and every visits operation declares header X-User-Id
        assertThat(paths.has("/api/countries")).isTrue();
        assertThat(paths.path("/api/countries/{code}").has("get")).isTrue();
        assertThat(paths.path("/api/visits").has("get")).isTrue();
        assertThat(paths.path("/api/visits").has("post")).isTrue();
        assertThat(paths.path("/api/visits/{code}").has("get")).isTrue();
        assertThat(paths.path("/api/visits/{code}").has("put")).isTrue();
        assertThat(paths.path("/api/visits/{code}").has("delete")).isTrue();

        List<String> operationsWithoutHeader = new ArrayList<>();
        for (String path : List.of("/api/visits", "/api/visits/{code}")) {
            paths.path(path).properties().forEach(operation -> {
                boolean hasHeader = false;
                for (var parameter : operation.getValue().path("parameters")) {
                    hasHeader |= parameter.path("in").asString().equals("header")
                            && parameter.path("name").asString().equals("X-User-Id");
                }
                if (!hasHeader) {
                    operationsWithoutHeader.add(operation.getKey() + " " + path);
                }
            });
        }
        assertThat(operationsWithoutHeader).isEmpty();
    }

    @Test
    void swaggerUi_andRootRedirect_areAvailable() {
        // Given the app has started
        // When GET /swagger-ui.html and GET / are called
        var swaggerUi = api().get("/swagger-ui/index.html");
        var root = api().get("/");

        // Then Swagger UI is served, and / redirects to it
        assertThat(swaggerUi.status()).isEqualTo(200);
        assertThat(swaggerUi.text()).containsIgnoringCase("swagger");
        assertThat(root.status()).isEqualTo(302);
        assertThat(root.location()).endsWith("/swagger-ui.html");
    }
}
