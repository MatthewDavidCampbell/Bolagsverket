package se.arbetsprov.countries.remote;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import se.arbetsprov.core.serialization.JsonSerializer;

/**
 * Reads the snapshot bundled with the library: a JSON array of v5 country objects taken from
 * REST Countries on 2026-11-04. Lets the application start without an API key or network.
 */
public final class SnapshotLoader {

    /** Classpath location of the bundled snapshot. */
    public static final String DEFAULT_RESOURCE = "/se/arbetsprov/countries/countries-snapshot.json";

    private final JsonSerializer serializer = new JsonSerializer();
    private final String resource;

    public SnapshotLoader() {
        this(DEFAULT_RESOURCE);
    }

    public SnapshotLoader(String resource) {
        this.resource = resource;
    }

    public List<V5Country> load() {
        try (var in = SnapshotLoader.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Snapshot not found on the classpath: " + resource);
            }
            return serializer.deserializeList(in.readAllBytes(), V5Country.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
