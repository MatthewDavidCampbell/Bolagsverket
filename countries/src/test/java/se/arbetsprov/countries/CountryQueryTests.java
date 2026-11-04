package se.arbetsprov.countries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static se.arbetsprov.countries.testing.Countries.country;
import static se.arbetsprov.countries.testing.Countries.numbered;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.countries.cache.InMemoryCountryCache;

/**
 * User story 2: paged lists with region filter and sorting, answered from the cache only.
 * The cache is filled with a small fixed set of countries; no network.
 */
@DisplayName("Country queries")
class CountryQueryTests {

    private static InMemoryCountryCache cacheWith(List<Country> countries) {
        var cache = new InMemoryCountryCache();
        cache.replaceAll(countries);
        return cache;
    }

    private static List<String> names(Page<Country> page) {
        return page.items().stream().map(Country::name).toList();
    }

    @Test
    void list_withDefaults_returnsFirst20SortedByNameAscending() {
        // Given 25 cached countries
        var shuffled = new ArrayList<>(numbered(25));
        java.util.Collections.reverse(shuffled);
        var cache = cacheWith(shuffled);

        // When listed with the default query
        var page = cache.list(CountryQuery.defaults());

        // Then 20 items are returned in name order, total 25, pages 2, page 0, size 20
        assertThat(names(page)).hasSize(20).startsWith("Country 01").endsWith("Country 20");
        assertThat(page.totalItems()).isEqualTo(25);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
    }

    @Test
    void list_secondPageOfSize2_returnsThirdAndFourthWithTotals() {
        // Given five cached countries
        var cache = cacheWith(numbered(5));

        // When page 1 with size 2 is listed
        var page = cache.list(CountryQuery.defaults().withPage(1, 2));

        // Then the third and fourth by name are returned, total 5, pages 3
        assertThat(names(page)).containsExactly("Country 03", "Country 04");
        assertThat(page.totalItems()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void list_pageBeyondEnd_returnsNoItemsButCorrectTotals() {
        // Given five cached countries
        var cache = cacheWith(numbered(5));

        // When page 10 with size 2 is listed
        var page = cache.list(CountryQuery.defaults().withPage(10, 2));

        // Then no items are returned, total 5, pages 3
        assertThat(page.items()).isEmpty();
        assertThat(page.totalItems()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void list_regionFilter_ignoresLetterCase() {
        // Given cached countries in Europe and Asia
        var cache = cacheWith(List.of(
                country("SWE", "Sweden", "Europe", 10), country("JPN", "Japan", "Asia", 120),
                country("NOR", "Norway", "Europe", 5)));

        // When listed with region "eUrOpE"
        var page = cache.list(CountryQuery.defaults().withRegion("eUrOpE"));

        // Then only the European countries are returned and the total counts only them
        assertThat(names(page)).containsExactly("Norway", "Sweden");
        assertThat(page.totalItems()).isEqualTo(2);
    }

    @Test
    void list_unknownRegion_returnsEmptyPage() {
        // Given cached countries
        var cache = cacheWith(numbered(3));

        // When listed with region "Atlantis"
        var page = cache.list(CountryQuery.defaults().withRegion("Atlantis"));

        // Then no items are returned, total 0, pages 0
        assertThat(page.items()).isEmpty();
        assertThat(page.totalItems()).isZero();
        assertThat(page.totalPages()).isZero();
    }

    @Test
    void list_blankRegion_meansNoFilter() {
        // Given cached countries in two regions
        var cache = cacheWith(List.of(country("SWE", "Sweden", "Europe", 10), country("JPN", "Japan", "Asia", 120)));

        // When listed with region "  "
        var page = cache.list(CountryQuery.defaults().withRegion("  "));

        // Then countries from both regions are returned
        assertThat(names(page)).containsExactly("Japan", "Sweden");
    }

    @Test
    void list_sortByNameDescending_returnsReverseNameOrder() {
        // Given cached Denmark, Finland and Sweden
        var cache = cacheWith(List.of(
                country("FIN", "Finland", "Europe", 5), country("SWE", "Sweden", "Europe", 10),
                country("DNK", "Denmark", "Europe", 6)));

        // When listed by name descending
        var page = cache.list(CountryQuery.defaults().withSort(CountrySort.NAME, SortDirection.DESC));

        // Then the order is Sweden, Finland, Denmark
        assertThat(names(page)).containsExactly("Sweden", "Finland", "Denmark");
    }

    @Test
    void list_sortByPopulationAscending_returnsSmallestFirst() {
        // Given cached countries with different populations
        var cache = cacheWith(List.of(
                country("SWE", "Sweden", "Europe", 10), country("ISL", "Iceland", "Europe", 1),
                country("DEU", "Germany", "Europe", 80)));

        // When listed by population ascending
        var page = cache.list(CountryQuery.defaults().withSort(CountrySort.POPULATION, SortDirection.ASC));

        // Then the least populous comes first
        assertThat(names(page)).containsExactly("Iceland", "Sweden", "Germany");
    }

    @Test
    void list_sortByPopulationDescending_breaksTiesByName() {
        // Given two countries with the same population, "Beta" and "Alpha", and one larger
        var cache = cacheWith(List.of(
                country("BBB", "Beta", "Europe", 5), country("AAA", "Alpha", "Europe", 5),
                country("GGG", "Gamma", "Europe", 9)));

        // When listed by population descending
        var page = cache.list(CountryQuery.defaults().withSort(CountrySort.POPULATION, SortDirection.DESC));

        // Then the larger comes first, then Alpha, then Beta
        assertThat(names(page)).containsExactly("Gamma", "Alpha", "Beta");
    }

    @Test
    void list_sortByName_placesAccentedNamesWithTheirBaseLetter() {
        // Given cached "Zambia", "Åland Islands" and "Albania"
        var cache = cacheWith(List.of(
                country("ZMB", "Zambia", "Africa", 1), country("ALA", "Åland Islands", "Europe", 1),
                country("ALB", "Albania", "Europe", 1)));

        // When listed by name ascending
        var page = cache.list(CountryQuery.defaults());

        // Then the order is Åland Islands, Albania, Zambia (not Zambia before Åland)
        assertThat(names(page)).containsExactly("Åland Islands", "Albania", "Zambia");
    }

    @Test
    void list_filterSortAndPageTogether_appliesFilterThenSortThenPage() {
        // Given six European and three Asian countries
        var countries = new ArrayList<Country>();
        for (int i = 1; i <= 6; i++) {
            countries.add(country("E0" + i, "Europe " + i, "Europe", i * 10L));
        }
        for (int i = 1; i <= 3; i++) {
            countries.add(country("A0" + i, "Asia " + i, "Asia", 1000L + i));
        }
        var cache = cacheWith(countries);

        // When page 1, size 2, region Europe, population descending is listed
        var page = cache.list(new CountryQuery("Europe", CountrySort.POPULATION, SortDirection.DESC, 1, 2));

        // Then the third and fourth most populous European countries are returned, total 6, pages 3
        assertThat(names(page)).containsExactly("Europe 4", "Europe 3");
        assertThat(page.totalItems()).isEqualTo(6);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void query_withInvalidPageOrSize_isRejectedNamingTheParameter() {
        // Given a page of -1, a size of 0 and a size of 101
        // When each query is created
        // Then each is rejected with an IllegalArgumentException naming "page" or "size"
        var defaults = CountryQuery.defaults();
        assertThatThrownBy(() -> defaults.withPage(-1, 20))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("page");
        assertThatThrownBy(() -> defaults.withPage(0, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("size");
        assertThatThrownBy(() -> defaults.withPage(0, 101))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("size");
    }

    @Test
    void list_beforeLoad_returnsEmptyPage() {
        // Given a client that has not loaded yet
        var client = new CountriesClient(new CountriesClientOptions());

        // When listed with the default query
        var page = client.list(CountryQuery.defaults());

        // Then no items are returned and total is 0
        assertThat(page.items()).isEmpty();
        assertThat(page.totalItems()).isZero();
    }
}
