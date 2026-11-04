package se.arbetsprov.core.http;

/**
 * A successful response whose body has been turned into the caller's type.
 *
 * @param status  the HTTP status code (2xx)
 * @param headers the response headers
 * @param value   the body as {@code T}; null for an empty single body, an empty list for an
 *                empty list body
 * @param <T>     the body type
 */
public record Response<T>(int status, HttpHeaders headers, T value) {
}
