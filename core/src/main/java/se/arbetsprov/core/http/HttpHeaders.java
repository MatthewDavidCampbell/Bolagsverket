package se.arbetsprov.core.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Immutable, case-insensitive HTTP headers. Every change returns a new instance, so headers can
 * be shared between threads and between policies without copying.
 */
public final class HttpHeaders {

    private static final HttpHeaders EMPTY = new HttpHeaders(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));

    private final Map<String, List<String>> values;

    private HttpHeaders(TreeMap<String, List<String>> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    /** Headers with no entries. */
    public static HttpHeaders empty() {
        return EMPTY;
    }

    /** Headers copied from a map of names to values. */
    public static HttpHeaders of(Map<String, List<String>> source) {
        var copy = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        source.forEach((name, list) -> copy.put(name, List.copyOf(list)));
        return new HttpHeaders(copy);
    }

    /** A copy with {@code value} added to any existing values of {@code name}. */
    public HttpHeaders with(String name, String value) {
        var copy = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        copy.putAll(values);
        var list = new ArrayList<>(copy.getOrDefault(name, List.of()));
        list.add(value);
        copy.put(name, List.copyOf(list));
        return new HttpHeaders(copy);
    }

    /** The first value of {@code name}, if present. */
    public Optional<String> first(String name) {
        return values(name).stream().findFirst();
    }

    /** All values of {@code name}; empty when the header is absent. */
    public List<String> values(String name) {
        return values.getOrDefault(name, List.of());
    }

    /** The header names. */
    public Set<String> names() {
        return values.keySet();
    }

    /** The number of distinct header names. */
    public int size() {
        return values.size();
    }

    /** A read-only view of all headers. */
    public Map<String, List<String>> asMap() {
        return values;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof HttpHeaders headers && values.equals(headers.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
