package se.arbetsprov.service;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import se.arbetsprov.service.testing.Api;

/**
 * User story 1 with an API key: the app imports from REST Countries (here a WireMock stand-in)
 * with the configured key before it serves requests. Separate from {@link StartupTests} because
 * it needs its own configuration.
 */
@DisplayName("Startup with live import")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LiveImportTests {

    private static final WireMockServer REST_COUNTRIES = new WireMockServer(options().dynamicPort());

    static {
        REST_COUNTRIES.start();
        REST_COUNTRIES.stubFor(get(urlPathEqualTo("/countries/v5")).willReturn(okJson("""
                {"data":{"objects":[
                  {"names":{"common":"Sweden","official":"Kingdom of Sweden"},"codes":{"alpha_3":"SWE"},
                   "capitals":[{"name":"Stockholm"}],"region":"Europe","subregion":"Northern Europe","population":10621103},
                  {"names":{"common":"Japan","official":"Japan"},"codes":{"alpha_3":"JPN"},
                   "capitals":[{"name":"Tokyo"}],"region":"Asia","subregion":"Eastern Asia","population":123000000}],
                 "meta":{"total":2,"count":2,"limit":100,"offset":0,"more":false}}}""")));
    }

    @DynamicPropertySource
    static void restCountries(DynamicPropertyRegistry registry) {
        registry.add("countries.base-uri", () -> REST_COUNTRIES.baseUrl() + "/countries/v5");
        registry.add("countries.api-key", () -> "test-key");
    }

    @AfterAll
    static void stop() {
        REST_COUNTRIES.stop();
    }

    @LocalServerPort
    int port;

    @Test
    void startup_withKey_importsLiveWithBearerKeyBeforeReady() {
        // Given a WireMock REST Countries with two countries, and the app started with
        // countries.base-uri pointing at it and countries.api-key "test-key"
        // When GET /api/countries is called
        var result = new Api(port).get("/api/countries");

        // Then exactly those two countries are listed, and WireMock saw "Authorization: Bearer test-key"
        assertThat(result.json().path("totalItems").asLong()).isEqualTo(2);
        assertThat(result.json().path("items").path(0).path("name").asString()).isEqualTo("Japan");
        REST_COUNTRIES.verify(getRequestedFor(urlPathEqualTo("/countries/v5"))
                .withHeader("Authorization", equalTo("Bearer test-key")));
    }
}
