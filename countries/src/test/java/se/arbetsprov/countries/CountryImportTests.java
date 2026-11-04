package se.arbetsprov.countries;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static se.arbetsprov.countries.testing.Countries.v5Countries;
import static se.arbetsprov.countries.testing.Countries.v5Country;
import static se.arbetsprov.countries.testing.Countries.v5Page;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.ServerSocket;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.core.credential.KeyCredential;

/**
 * User stories 1 and 3: countries are imported at start-up, or taken from the bundled snapshot.
 * REST Countries is stubbed with WireMock; the real service is never called.
 */
@DisplayName("Country import")
class CountryImportTests {

    private static final String LIST_PATH = "/countries/v5";
    private static final int SNAPSHOT_COUNT = 250;

    private final WireMockServer server = new WireMockServer(options().dynamicPort());

    @BeforeEach
    void start() {
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    private CountriesClient client(String key) {
        return new CountriesClient(new CountriesClientOptions()
                .baseUri(URI.create(server.baseUrl() + LIST_PATH))
                .credential(KeyCredential.ofNullable(key)));
    }

    private void stubPage(int offset, String body) {
        server.stubFor(get(urlPathEqualTo(LIST_PATH))
                .withQueryParam("offset", equalTo(String.valueOf(offset)))
                .willReturn(okJson(body)));
    }

    @Test
    void load_withKeyAndThreePages_cachesCountriesFromEveryPage() {
        // Given an API key and a stub serving pages at offset 0, 100 and 200 (the last with "more": false)
        stubPage(0, v5Page(v5Countries(0, 100), 250, 0, true));
        stubPage(100, v5Page(v5Countries(100, 100), 250, 100, true));
        stubPage(200, v5Page(v5Countries(200, 50), 250, 200, false));
        var client = client("test-key");

        // When the client loads
        var result = client.load();

        // Then the cache holds the countries of all three pages and the result says LIVE
        assertThat(result.source()).isEqualTo(LoadResult.Source.LIVE);
        assertThat(result.count()).isEqualTo(250);
        assertThat(client.get("X000")).isPresent();
        assertThat(client.get("X249")).isPresent();
        server.verify(3, getRequestedFor(urlPathEqualTo(LIST_PATH)));
    }

    @Test
    void load_withKey_sendsBearerKeyAndAsksOnlyForNeededFields() {
        // Given API key "test-key" and a one-page stub
        stubPage(0, v5Page(v5Countries(0, 2), 2, 0, false));

        // When the client loads
        client("test-key").load();

        // Then every request carried "Authorization: Bearer test-key", limit=100 and the response_fields list
        server.verify(getRequestedFor(urlPathEqualTo(LIST_PATH))
                .withHeader("Authorization", equalTo("Bearer test-key"))
                .withQueryParam("limit", equalTo("100"))
                .withQueryParam("response_fields", equalTo(
                        "names.common,names.official,codes.alpha_3,region,subregion,capitals.name,population")));
    }

    @Test
    void load_entriesWithoutAlpha3Code_areSkippedAndCounted() {
        // Given a page with three countries, one of which has an empty alpha_3 code
        stubPage(0, v5Page(List.of(
                v5Country("SWE", "Sweden", "Europe", "Stockholm", 10_000_000),
                v5Country("", "Somaliland", "Africa", "Hargeisa", 5_000_000),
                v5Country("NOR", "Norway", "Europe", "Oslo", 5_500_000)), 3, 0, false));
        var client = client("test-key");

        // When the client loads
        var result = client.load();

        // Then two countries are cached and the result reports one skipped
        assertThat(result.count()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(client.list(CountryQuery.defaults()).items()).extracting(Country::code).containsExactly("NOR", "SWE");
    }

    @Test
    void load_countryWithoutCapital_isCachedWithNoCapital() {
        // Given a page with a country whose capitals list is empty
        stubPage(0, v5Page(List.of(v5Country("ATA", "Antarctica", "Antarctic", null, 1000)), 1, 0, false));
        var client = client("test-key");

        // When the client loads
        client.load();

        // Then that country is cached and its capital is null
        assertThat(client.get("ATA")).hasValueSatisfying(c -> assertThat(c.capital()).isNull());
    }

    @Test
    void load_withoutKey_usesSnapshotWithoutCallingService() {
        // Given no API key
        var client = client(null);

        // When the client loads
        var result = client.load();

        // Then the stub received no requests, the cache holds 250 countries and the result says SNAPSHOT
        server.verify(0, anyRequestedFor(anyUrl()));
        assertThat(result.source()).isEqualTo(LoadResult.Source.SNAPSHOT);
        assertThat(result.count()).isEqualTo(SNAPSHOT_COUNT);
        assertThat(result.reason()).contains("no REST Countries API key");
    }

    @Test
    void load_whenServiceAnswers401_fallsBackToSnapshot() {
        // Given an API key the stub rejects with 401
        server.stubFor(get(urlPathEqualTo(LIST_PATH)).willReturn(aResponse().withStatus(401)));
        var client = client("bad-key");

        // When the client loads
        var result = client.load();

        // Then the cache holds the snapshot's 250 countries and the result says SNAPSHOT with the cause
        assertThat(result.source()).isEqualTo(LoadResult.Source.SNAPSHOT);
        assertThat(result.count()).isEqualTo(SNAPSHOT_COUNT);
        assertThat(result.reason()).contains("401").doesNotContain("bad-key");
    }

    @Test
    void load_whenServiceUnreachable_fallsBackToSnapshot() throws Exception {
        // Given an API key and a base URI where nothing listens
        int freePort;
        try (var socket = new ServerSocket(0)) {
            freePort = socket.getLocalPort();
        }
        var client = new CountriesClient(new CountriesClientOptions()
                .baseUri(URI.create("http://localhost:" + freePort + LIST_PATH))
                .credential(new KeyCredential("test-key")));

        // When the client loads
        var result = client.load();

        // Then the cache holds the snapshot's 250 countries and the result says SNAPSHOT
        assertThat(result.source()).isEqualTo(LoadResult.Source.SNAPSHOT);
        assertThat(result.count()).isEqualTo(SNAPSHOT_COUNT);
    }

    @Test
    void load_whenBodyIsMalformed_fallsBackToSnapshot() {
        // Given an API key and a stub answering 200 with a body that is not a v5 page
        server.stubFor(get(urlPathEqualTo(LIST_PATH)).willReturn(okJson("[1, 2, 3]")));

        // When the client loads
        var result = client("test-key").load();

        // Then the cache holds the snapshot's 250 countries and the result says SNAPSHOT
        assertThat(result.source()).isEqualTo(LoadResult.Source.SNAPSHOT);
        assertThat(result.count()).isEqualTo(SNAPSHOT_COUNT);
    }

    @Test
    void load_whenSecondPageFails_keepsNoPartialLiveData() {
        // Given a stub whose first page succeeds and second page answers 500
        stubPage(0, v5Page(v5Countries(0, 100), 250, 0, true));
        server.stubFor(get(urlPathEqualTo(LIST_PATH))
                .withQueryParam("offset", equalTo("100"))
                .willReturn(aResponse().withStatus(500)));
        var client = client("test-key");

        // When the client loads
        var result = client.load();

        // Then the cache holds the snapshot, not the first page's countries
        assertThat(result.source()).isEqualTo(LoadResult.Source.SNAPSHOT);
        assertThat(client.get("X000")).isEmpty();
        assertThat(client.get("SWE")).isPresent();
    }
}
