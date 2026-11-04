package se.arbetsprov.countries;

import java.util.Objects;

/**
 * A country as held in the cache: a flat, reshaped view of what REST Countries returns.
 *
 * @param code         ISO 3166-1 alpha-3 code, upper case (for example "SWE")
 * @param name         common English name
 * @param officialName official English name
 * @param region       region, for example "Europe"
 * @param subregion    subregion, for example "Northern Europe"; may be null
 * @param capital      first listed capital; null when the country has none
 * @param population   population
 */
public record Country(
        String code,
        String name,
        String officialName,
        String region,
        String subregion,
        String capital,
        long population) {

    public Country {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(region, "region");
    }
}
