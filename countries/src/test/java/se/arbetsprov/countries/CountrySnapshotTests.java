package se.arbetsprov.countries;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.countries.remote.CountryMapper;
import se.arbetsprov.countries.remote.SnapshotLoader;

/**
 * User story 3: the bundled snapshot is valid on its own.
 */
@DisplayName("Country snapshot")
class CountrySnapshotTests {

    private static final CountryMapper.Mapped SNAPSHOT = CountryMapper.map(new SnapshotLoader().load());

    @Test
    void snapshot_loads250CountriesWithUniqueCodes() {
        // Given the bundled snapshot
        // When it is loaded and mapped
        var codes = SNAPSHOT.countries().stream().map(Country::code).toList();

        // Then there are 250 countries, every code is three upper-case letters and no code repeats
        assertThat(codes).hasSize(250).doesNotHaveDuplicates().allMatch(c -> c.matches("[A-Z]{3}"));
        assertThat(SNAPSHOT.skipped()).isEqualTo(4);
    }

    @Test
    void snapshot_coversTheSixRegions() {
        // Given the bundled snapshot
        // When its regions are collected
        var regions = SNAPSHOT.countries().stream().map(Country::region).collect(Collectors.toSet());

        // Then they are exactly Africa, Americas, Antarctic, Asia, Europe and Oceania
        assertThat(regions).containsExactlyInAnyOrder("Africa", "Americas", "Antarctic", "Asia", "Europe", "Oceania");
    }

    @Test
    void snapshot_containsSwedenWithExpectedFields() {
        // Given the bundled snapshot
        // When SWE is looked up
        var sweden = SNAPSHOT.countries().stream().filter(c -> c.code().equals("SWE")).findFirst();

        // Then its name is "Sweden", region "Europe", subregion "Northern Europe", capital "Stockholm"
        // and population is above 10 million
        assertThat(sweden).hasValueSatisfying(c -> {
            assertThat(c.name()).isEqualTo("Sweden");
            assertThat(c.region()).isEqualTo("Europe");
            assertThat(c.subregion()).isEqualTo("Northern Europe");
            assertThat(c.capital()).isEqualTo("Stockholm");
            assertThat(c.population()).isGreaterThan(10_000_000);
        });
    }
}
