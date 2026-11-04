package se.arbetsprov.countries;

import java.lang.System.Logger.Level;
import java.util.Optional;
import se.arbetsprov.countries.cache.CountryCache;
import se.arbetsprov.countries.cache.InMemoryCountryCache;
import se.arbetsprov.countries.remote.CountryMapper;
import se.arbetsprov.countries.remote.RestCountriesImporter;
import se.arbetsprov.countries.remote.SnapshotLoader;

/**
 * Entry point of the countries service library.
 *
 * <p>{@link #load()} runs once at start-up: it imports every country from REST Countries through
 * the core pipeline into the cache, or falls back to the bundled snapshot. After that,
 * {@link #list(CountryQuery)} and {@link #get(String)} answer from the cache only and never call
 * REST Countries.
 */
public final class CountriesClient {

    private static final System.Logger LOG = System.getLogger(CountriesClient.class.getName());

    private final CountriesClientOptions options;
    private final CountryCache cache;

    public CountriesClient(CountriesClientOptions options) {
        this(options, new InMemoryCountryCache());
    }

    public CountriesClient(CountriesClientOptions options, CountryCache cache) {
        this.options = options;
        this.cache = cache;
    }

    /**
     * Fills the cache: live from REST Countries when a credential is configured, otherwise, or on
     * any failure, from the bundled snapshot. Never throws for a remote failure.
     */
    public LoadResult load() {
        if (options.credential() == null) {
            return loadSnapshot("no REST Countries API key configured");
        }
        try {
            var importer = new RestCountriesImporter(options.buildPipeline(), options.baseUri(), options.pageSize());
            var mapped = CountryMapper.map(importer.fetchAll());
            if (mapped.countries().isEmpty()) {
                return loadSnapshot("REST Countries returned no countries");
            }
            cache.replaceAll(mapped.countries());
            LOG.log(Level.INFO, "Imported {0} countries from REST Countries ({1} without ISO code skipped)",
                    mapped.countries().size(), mapped.skipped());
            return new LoadResult(LoadResult.Source.LIVE, mapped.countries().size(), mapped.skipped(), null);
        } catch (RuntimeException e) {
            return loadSnapshot("import from REST Countries failed: " + e.getMessage());
        }
    }

    /** One page of countries from the cache. */
    public Page<Country> list(CountryQuery query) {
        return cache.list(query);
    }

    /** The country with this ISO alpha-3 code, ignoring letter case. */
    public Optional<Country> get(String code) {
        return cache.get(code);
    }

    private LoadResult loadSnapshot(String reason) {
        var mapped = CountryMapper.map(new SnapshotLoader(options.snapshotResource()).load());
        cache.replaceAll(mapped.countries());
        LOG.log(Level.WARNING, "Using bundled country snapshot ({0} countries): {1}",
                mapped.countries().size(), reason);
        return new LoadResult(LoadResult.Source.SNAPSHOT, mapped.countries().size(), mapped.skipped(), reason);
    }
}
