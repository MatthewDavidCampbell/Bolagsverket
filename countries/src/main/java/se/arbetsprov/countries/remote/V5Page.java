package se.arbetsprov.countries.remote;

import java.util.List;

/**
 * One page of the REST Countries v5 list endpoint.
 *
 * @param data the objects and the paging metadata
 */
public record V5Page(Data data) {

    public record Data(List<V5Country> objects, Meta meta) {
    }

    /**
     * @param total  countries across all pages
     * @param offset offset of this page
     * @param more   whether there are countries after this page
     */
    public record Meta(int total, int count, int limit, int offset, boolean more) {
    }
}
