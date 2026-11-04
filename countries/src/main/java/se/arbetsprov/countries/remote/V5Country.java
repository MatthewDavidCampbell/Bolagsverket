package se.arbetsprov.countries.remote;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * One country as REST Countries v5 returns it, limited to the requested response fields.
 * Only used on the way in; the cache holds {@link se.arbetsprov.countries.Country}.
 */
public record V5Country(
        Names names,
        Codes codes,
        List<Capital> capitals,
        String region,
        String subregion,
        Long population) {

    public record Names(String common, String official) {
    }

    public record Codes(@JsonProperty("alpha_3") String alpha3) {
    }

    public record Capital(String name) {
    }
}
