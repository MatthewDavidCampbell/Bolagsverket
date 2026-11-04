package se.arbetsprov.countries;

import java.util.List;

/**
 * One page of a larger, already filtered and sorted result.
 *
 * @param items      the items on this page; empty for a page past the end
 * @param page       page number, from 0
 * @param size       requested page size
 * @param totalItems number of items across all pages
 * @param totalPages number of pages
 * @param <T>        item type
 */
public record Page<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public Page {
        items = List.copyOf(items);
    }

    /** Cuts page {@code page} of size {@code size} out of {@code all}. */
    public static <T> Page<T> of(List<T> all, int page, int size) {
        int total = all.size();
        int from = (int) Math.min((long) page * size, total);
        int to = Math.min(from + size, total);
        int pages = (total + size - 1) / size;
        return new Page<>(all.subList(from, to), page, size, total, pages);
    }
}
