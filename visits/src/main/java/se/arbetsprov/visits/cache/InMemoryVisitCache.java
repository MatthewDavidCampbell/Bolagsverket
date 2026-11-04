package se.arbetsprov.visits.cache;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import se.arbetsprov.visits.Visit;

/**
 * Visits in memory: a map from user id to that user's map from country code to visit. Atomic
 * {@link ConcurrentHashMap} operations ({@code putIfAbsent}, {@code computeIfPresent},
 * {@code remove}) replace locks, so many virtual threads can work at once and two calls only
 * contend when they touch the same visit.
 */
public final class InMemoryVisitCache implements VisitCache {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Visit>> visitsByUser = new ConcurrentHashMap<>();

    @Override
    public Optional<Visit> putIfAbsent(Visit visit) {
        var visits = visitsByUser.computeIfAbsent(visit.userId(), id -> new ConcurrentHashMap<>());
        return Optional.ofNullable(visits.putIfAbsent(visit.countryCode(), visit));
    }

    @Override
    public Optional<Visit> get(String userId, String countryCode) {
        return Optional.ofNullable(visitsOf(userId).get(countryCode));
    }

    @Override
    public Optional<Visit> update(String userId, String countryCode, UnaryOperator<Visit> change) {
        return Optional.ofNullable(visitsOf(userId).computeIfPresent(countryCode, (code, visit) -> change.apply(visit)));
    }

    @Override
    public boolean remove(String userId, String countryCode) {
        return visitsOf(userId).remove(countryCode) != null;
    }

    @Override
    public List<Visit> list(String userId) {
        return visitsOf(userId).values().stream()
                .sorted(Comparator.comparing(Visit::countryCode))
                .toList();
    }

    private Map<String, Visit> visitsOf(String userId) {
        return visitsByUser.getOrDefault(userId, new ConcurrentHashMap<>());
    }
}
