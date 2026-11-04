package se.arbetsprov.countries;

/**
 * What a load put into the cache.
 *
 * @param source  where the countries came from
 * @param count   countries cached
 * @param skipped entries left out because they have no ISO alpha-3 code
 * @param reason  for {@link Source#SNAPSHOT}, why the live import was not used; otherwise null
 */
public record LoadResult(Source source, int count, int skipped, String reason) {

    /** Where cached countries came from. */
    public enum Source {
        /** Imported from REST Countries at start-up. */
        LIVE,
        /** Read from the snapshot bundled with the library. */
        SNAPSHOT
    }
}
