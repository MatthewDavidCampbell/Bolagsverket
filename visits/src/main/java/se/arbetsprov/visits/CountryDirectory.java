package se.arbetsprov.visits;

/**
 * Answers whether a country code exists. Supplied by the host application (which wires it to the
 * countries library), so visits does not depend on countries directly.
 */
@FunctionalInterface
public interface CountryDirectory {

    /** True when {@code code} (upper-case ISO alpha-3) is a known country. */
    boolean exists(String code);
}
