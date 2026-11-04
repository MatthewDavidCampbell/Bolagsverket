package se.arbetsprov.countries;

import java.net.URI;
import se.arbetsprov.core.client.ClientOptions;
import se.arbetsprov.core.credential.KeyCredential;
import se.arbetsprov.core.http.HttpClient;
import se.arbetsprov.core.http.policy.HttpPipelinePolicy;
import se.arbetsprov.countries.remote.SnapshotLoader;

/**
 * Settings for {@link CountriesClient}. Defaults to the REST Countries v5 list endpoint, pages of
 * 100 (the free plan's maximum) and the bundled snapshot. Without a credential the client does
 * not call REST Countries at all and loads the snapshot.
 */
public final class CountriesClientOptions extends ClientOptions {

    /** The REST Countries v5 list endpoint. */
    public static final URI DEFAULT_BASE_URI = URI.create("https://api.restcountries.com/countries/v5");

    private int pageSize = 100;
    private String snapshotResource = SnapshotLoader.DEFAULT_RESOURCE;

    public CountriesClientOptions() {
        super.baseUri(DEFAULT_BASE_URI);
    }

    /** Countries per call to REST Countries, 1 to 100. */
    public CountriesClientOptions pageSize(int pageSize) {
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100, was " + pageSize);
        }
        this.pageSize = pageSize;
        return this;
    }

    public int pageSize() {
        return pageSize;
    }

    /** Classpath location of the fallback snapshot. */
    public CountriesClientOptions snapshotResource(String snapshotResource) {
        this.snapshotResource = snapshotResource;
        return this;
    }

    public String snapshotResource() {
        return snapshotResource;
    }

    @Override
    public CountriesClientOptions baseUri(URI baseUri) {
        super.baseUri(baseUri);
        return this;
    }

    @Override
    public CountriesClientOptions credential(KeyCredential credential) {
        super.credential(credential);
        return this;
    }

    @Override
    public CountriesClientOptions httpClient(HttpClient httpClient) {
        super.httpClient(httpClient);
        return this;
    }

    @Override
    public CountriesClientOptions policies(HttpPipelinePolicy... policies) {
        super.policies(policies);
        return this;
    }
}
