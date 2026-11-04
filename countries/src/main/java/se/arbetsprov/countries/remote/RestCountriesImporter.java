package se.arbetsprov.countries.remote;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import se.arbetsprov.core.http.HttpPipeline;
import se.arbetsprov.core.http.HttpRequest;

/**
 * Reads every country from REST Countries v5, page by page, through the core pipeline (which adds
 * the API key). Asks only for the fields the cache keeps.
 */
public final class RestCountriesImporter {

    static final String RESPONSE_FIELDS =
            "names.common,names.official,codes.alpha_3,region,subregion,capitals.name,population";

    // Guards against a service that keeps answering "more": 254 countries need 3 pages of 100.
    private static final int MAX_PAGES = 20;

    private final HttpPipeline pipeline;
    private final URI listUri;
    private final int pageSize;

    /**
     * @param pipeline pipeline with transport and policies
     * @param listUri  the v5 list endpoint, for example {@code https://api.restcountries.com/countries/v5}
     * @param pageSize countries per call; at most 100 on the free plan
     */
    public RestCountriesImporter(HttpPipeline pipeline, URI listUri, int pageSize) {
        this.pipeline = pipeline;
        this.listUri = listUri;
        this.pageSize = pageSize;
    }

    /**
     * Fetches all pages. Any failure (status, transport, body) propagates, so callers never get a
     * partial list.
     */
    public List<V5Country> fetchAll() {
        var all = new ArrayList<V5Country>();
        int offset = 0;
        for (int pages = 0; pages < MAX_PAGES; pages++) {
            V5Page.Data data = pipeline.send(HttpRequest.get(pageUri(offset)), V5Page.class).value().data();
            if (data == null || data.objects() == null) {
                throw new IllegalStateException("REST Countries returned a page without objects at offset " + offset);
            }
            all.addAll(data.objects());
            if (data.meta() == null || !data.meta().more() || data.objects().isEmpty()) {
                return all;
            }
            offset += data.objects().size();
        }
        throw new IllegalStateException("REST Countries still reported more data after " + MAX_PAGES + " pages");
    }

    private URI pageUri(int offset) {
        // Built as text: URI.resolve with a query-only reference drops the last path segment.
        return URI.create(listUri + "?limit=" + pageSize + "&offset=" + offset + "&response_fields=" + RESPONSE_FIELDS);
    }
}
