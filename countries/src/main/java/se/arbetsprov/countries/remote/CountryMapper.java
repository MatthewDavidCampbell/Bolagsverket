package se.arbetsprov.countries.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import se.arbetsprov.countries.Country;

/**
 * Reshapes v5's nested objects into flat {@link Country} records. Entries without an ISO
 * alpha-3 code (a few partially recognised territories) are left out, because the cache and
 * visits are keyed by that code.
 */
public final class CountryMapper {

    /**
     * @param countries mapped countries
     * @param skipped   entries left out for having no alpha-3 code
     */
    public record Mapped(List<Country> countries, int skipped) {
    }

    private CountryMapper() {
    }

    public static Mapped map(List<V5Country> remote) {
        var countries = new ArrayList<Country>(remote.size());
        int skipped = 0;
        for (V5Country r : remote) {
            String code = r.codes() == null ? null : r.codes().alpha3();
            if (code == null || code.isBlank() || r.names() == null || r.region() == null) {
                skipped++;
                continue;
            }
            countries.add(new Country(
                    code.toUpperCase(Locale.ROOT),
                    r.names().common(),
                    r.names().official(),
                    r.region(),
                    r.subregion(),
                    r.capitals() == null || r.capitals().isEmpty() ? null : r.capitals().getFirst().name(),
                    r.population() == null ? 0 : r.population()));
        }
        return new Mapped(List.copyOf(countries), skipped);
    }
}
