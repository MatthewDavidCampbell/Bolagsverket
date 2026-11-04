package se.arbetsprov.visits;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import se.arbetsprov.visits.cache.InMemoryVisitCache;
import se.arbetsprov.visits.cache.VisitCache;

/**
 * Entry point of the visits service library: create, read, update, delete and list a user's
 * visited countries. Every call is scoped to one user id, which is taken as given (authenticated
 * elsewhere).
 */
public final class VisitsClient {

    /** Longest note accepted, after trimming. */
    public static final int MAX_NOTE_LENGTH = 500;

    /** Longest user id accepted. */
    public static final int MAX_USER_ID_LENGTH = 64;

    private final VisitsClientOptions options;
    private final CountryDirectory countries;
    private final VisitCache cache;

    /**
     * @param options   backend and clock
     * @param countries answers whether a country code exists
     */
    public VisitsClient(VisitsClientOptions options, CountryDirectory countries) {
        this.options = options;
        this.countries = Objects.requireNonNull(countries, "countries");
        this.cache = switch (options.backend()) {
            case IN_PROCESS -> new InMemoryVisitCache();
        };
    }

    /**
     * Marks a country as visited.
     *
     * @throws UnknownCountryException      when the country code is not known
     * @throws VisitAlreadyExistsException  when the user already marked it; the visit is unchanged
     * @throws IllegalArgumentException     for an invalid user id or note
     */
    public Visit create(String userId, String countryCode, String note) {
        String user = requireUserId(userId);
        String code = normalizeCode(countryCode);
        String text = normalizeNote(note);
        if (!countries.exists(code)) {
            throw new UnknownCountryException(code);
        }
        var now = options.clock().instant();
        var visit = new Visit(user, code, text, now, now);
        if (cache.putIfAbsent(visit).isPresent()) {
            throw new VisitAlreadyExistsException(code);
        }
        return visit;
    }

    /** The user's visit to a country, if any. */
    public Optional<Visit> get(String userId, String countryCode) {
        return cache.get(requireUserId(userId), normalizeCode(countryCode));
    }

    /**
     * Replaces the note of an existing visit; a blank note removes it.
     *
     * @throws VisitNotFoundException   when the user has not marked the country
     * @throws IllegalArgumentException for an invalid user id or note
     */
    public Visit updateNote(String userId, String countryCode, String note) {
        String user = requireUserId(userId);
        String code = normalizeCode(countryCode);
        String text = normalizeNote(note);
        var now = options.clock().instant();
        return cache.update(user, code, visit -> visit.withNote(text, now))
                .orElseThrow(() -> new VisitNotFoundException(code));
    }

    /** Unmarks a country; true when a visit was removed, false when there was none. */
    public boolean delete(String userId, String countryCode) {
        return cache.remove(requireUserId(userId), normalizeCode(countryCode));
    }

    /** All of the user's visits, ordered by country code. */
    public List<Visit> list(String userId) {
        return cache.list(requireUserId(userId));
    }

    private static String requireUserId(String userId) {
        if (userId == null || userId.isBlank() || userId.length() > MAX_USER_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "userId must be 1 to " + MAX_USER_ID_LENGTH + " characters and not blank");
        }
        return userId;
    }

    private static String normalizeCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            throw new IllegalArgumentException("countryCode must not be blank");
        }
        return countryCode.strip().toUpperCase(Locale.ROOT);
    }

    private static String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String text = note.strip();
        if (text.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException(
                    "note must be at most " + MAX_NOTE_LENGTH + " characters, was " + text.length());
        }
        return text;
    }
}
