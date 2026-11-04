package se.arbetsprov.countries.cache;

import java.util.Collection;
import java.util.Optional;
import se.arbetsprov.countries.Country;
import se.arbetsprov.countries.CountryQuery;
import se.arbetsprov.countries.Page;

/**
 * Countries held locally, so reads never reach the remote service. The in-memory version is the
 * only one in this sample; a shared cache (for example Redis) would sit behind the same interface
 * when the service runs as several replicas.
 */
public interface CountryCache {

    /** Replaces all contents in one step; readers see either the old or the new set. */
    void replaceAll(Collection<Country> countries);

    /** One page of countries matching the query. */
    Page<Country> list(CountryQuery query);

    /** The country with this ISO alpha-3 code, ignoring letter case. */
    Optional<Country> get(String code);

    /** Number of cached countries. */
    int size();
}
