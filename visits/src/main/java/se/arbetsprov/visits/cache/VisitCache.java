package se.arbetsprov.visits.cache;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import se.arbetsprov.visits.Visit;

/**
 * Where visits are kept, always addressed per user. Every method is atomic for one visit.
 */
public interface VisitCache {

    /** Stores {@code visit} unless the user already has one for that country; returns the existing one if so. */
    Optional<Visit> putIfAbsent(Visit visit);

    /** The user's visit to {@code countryCode}, if any. */
    Optional<Visit> get(String userId, String countryCode);

    /** Applies {@code change} to an existing visit; empty when there is none. */
    Optional<Visit> update(String userId, String countryCode, UnaryOperator<Visit> change);

    /** Removes the user's visit; true when one was removed. */
    boolean remove(String userId, String countryCode);

    /** All of the user's visits, ordered by country code. */
    List<Visit> list(String userId);
}
