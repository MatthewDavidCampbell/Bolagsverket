package se.arbetsprov.countries.cache;

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import se.arbetsprov.countries.Country;
import se.arbetsprov.countries.CountryQuery;
import se.arbetsprov.countries.Page;
import se.arbetsprov.countries.SortDirection;

/**
 * Keeps countries in an immutable list plus a code index. Queries are plain Java Streams
 * (filter, sorted, then a page slice), the equivalent of LINQ's Where, OrderBy, Skip and Take.
 *
 * <p>The list and index are replaced together through one volatile reference, so readers need
 * no locks and never see a half-loaded cache.
 */
public final class InMemoryCountryCache implements CountryCache {

    private record Contents(List<Country> countries, Map<String, Country> byCode) {
    }

    private volatile Contents contents = new Contents(List.of(), Map.of());

    @Override
    public void replaceAll(Collection<Country> countries) {
        var list = List.copyOf(countries);
        var index = list.stream().collect(Collectors.toUnmodifiableMap(
                c -> key(c.code()), Function.identity()));
        contents = new Contents(list, index);
    }

    @Override
    public Page<Country> list(CountryQuery query) {
        var matching = contents.countries().stream()
                .filter(c -> query.region() == null || c.region().equalsIgnoreCase(query.region()))
                .sorted(comparator(query))
                .toList();
        return Page.of(matching, query.page(), query.size());
    }

    @Override
    public Optional<Country> get(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(contents.byCode().get(key(code)));
    }

    @Override
    public int size() {
        return contents.countries().size();
    }

    private static String key(String code) {
        return code.strip().toUpperCase(Locale.ROOT);
    }

    private static Comparator<Country> comparator(CountryQuery query) {
        // A collator puts "Åland Islands" with the A's; a plain String compare puts it after Z.
        // Collators are not thread-safe, so each query gets its own.
        Collator collator = Collator.getInstance(Locale.ENGLISH);
        collator.setStrength(Collator.PRIMARY);
        Comparator<Country> byName = Comparator.comparing(Country::name, collator).thenComparing(Country::code);

        return switch (query.sort()) {
            case NAME -> query.direction() == SortDirection.ASC ? byName : byName.reversed();
            case POPULATION -> {
                Comparator<Country> byPopulation = Comparator.comparingLong(Country::population);
                if (query.direction() == SortDirection.DESC) {
                    byPopulation = byPopulation.reversed();
                }
                // Equal populations always fall back to name ascending.
                yield byPopulation.thenComparing(byName);
            }
        };
    }
}
