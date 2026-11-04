package se.arbetsprov.visits;

import java.time.Instant;
import java.util.Objects;

/**
 * A country a user has marked as visited.
 *
 * @param userId      the user, as given by the caller (authenticated elsewhere)
 * @param countryCode ISO alpha-3 code, upper case
 * @param note        short note; null when there is none
 * @param createdAt   when the country was marked
 * @param updatedAt   when the note last changed (equal to {@code createdAt} until then)
 */
public record Visit(String userId, String countryCode, String note, Instant createdAt, Instant updatedAt) {

    public Visit {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(countryCode, "countryCode");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** A copy with a new note, updated at {@code now}. */
    public Visit withNote(String note, Instant now) {
        return new Visit(userId, countryCode, note, createdAt, now);
    }
}
