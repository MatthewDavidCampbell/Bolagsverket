package se.arbetsprov.service.countries;

import java.util.List;
import java.util.function.Function;
import se.arbetsprov.countries.Page;

/**
 * One page of results.
 *
 * @param items      items on this page
 * @param page       page number, from 0
 * @param size       page size
 * @param totalItems items across all pages
 * @param totalPages number of pages
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> map) {
        return new PageResponse<>(page.items().stream().map(map).toList(),
                page.page(), page.size(), page.totalItems(), page.totalPages());
    }
}
