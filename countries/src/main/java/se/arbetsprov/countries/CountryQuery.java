package se.arbetsprov.countries;

import java.util.Objects;

/**
 * A request for one page of countries.
 *
 * @param region    region to filter on, ignoring letter case; null or blank for all regions
 * @param sort      what to sort by
 * @param direction sort direction
 * @param page      page number, from 0
 * @param size      page size, 1 to {@value #MAX_SIZE}
 */
public record CountryQuery(String region, CountrySort sort, SortDirection direction, int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public CountryQuery {
        region = region == null || region.isBlank() ? null : region.strip();
        Objects.requireNonNull(sort, "sort");
        Objects.requireNonNull(direction, "direction");
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or greater, was " + page);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE + ", was " + size);
        }
    }

    /** All regions, sorted by name ascending, first page of {@value #DEFAULT_SIZE}. */
    public static CountryQuery defaults() {
        return new CountryQuery(null, CountrySort.NAME, SortDirection.ASC, 0, DEFAULT_SIZE);
    }

    /** A copy filtered on {@code region}. */
    public CountryQuery withRegion(String region) {
        return new CountryQuery(region, sort, direction, page, size);
    }

    /** A copy with a different sort. */
    public CountryQuery withSort(CountrySort sort, SortDirection direction) {
        return new CountryQuery(region, sort, direction, page, size);
    }

    /** A copy for a different page. */
    public CountryQuery withPage(int page, int size) {
        return new CountryQuery(region, sort, direction, page, size);
    }
}
