package se.arbetsprov.countries;

import static org.assertj.core.api.Assertions.assertThat;
import static se.arbetsprov.countries.testing.Countries.country;
import static se.arbetsprov.countries.testing.Countries.numbered;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.countries.cache.InMemoryCountryCache;

/**
 * User story 4: look up one country by its ISO alpha-3 code.
 */
@DisplayName("Country lookup")
class CountryLookupTests {

    @Test
    void get_byCodeInAnyLetterCase_returnsCountry() {
        // Given cached Sweden (SWE)
        var cache = new InMemoryCountryCache();
        cache.replaceAll(List.of(country("SWE", "Sweden", "Europe", 10)));

        // When "swe" is looked up
        // Then Sweden is returned
        assertThat(cache.get("swe")).map(Country::name).contains("Sweden");
    }

    @Test
    void get_unknownCode_returnsEmpty() {
        // Given cached countries without XYZ
        var cache = new InMemoryCountryCache();
        cache.replaceAll(numbered(3));

        // When "XYZ" is looked up
        // Then nothing is returned, and no exception is thrown
        assertThat(cache.get("XYZ")).isEmpty();
        assertThat(cache.get(null)).isEmpty();
    }

    @Test
    void load_twice_replacesContentsInOneStep() throws Exception {
        // Given a cache holding set A, and readers listing on other virtual threads
        var setA = numbered(50);
        var setB = setA.stream()
                .map(c -> country(c.code(), c.name().replace("Country", "Land"), c.region(), c.population()))
                .toList();
        var cache = new InMemoryCountryCache();
        cache.replaceAll(setA);
        var query = CountryQuery.defaults().withPage(0, 100);
        var running = new AtomicBoolean(true);
        var mixed = new ConcurrentLinkedQueue<String>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int r = 0; r < 8; r++) {
                executor.submit(() -> {
                    while (running.get()) {
                        var prefixes = cache.list(query).items().stream()
                                .map(c -> c.name().split(" ")[0]).collect(Collectors.toSet());
                        if (prefixes.size() != 1) {
                            mixed.add(prefixes.toString());
                        }
                    }
                });
            }

            // When set B is loaded (repeatedly swapping with A)
            for (int i = 0; i < 200; i++) {
                cache.replaceAll(i % 2 == 0 ? setB : setA);
            }
            running.set(false);
        }

        // Then every reader sees either all of A or all of B, never a mix
        assertThat(mixed).isEmpty();
    }
}
