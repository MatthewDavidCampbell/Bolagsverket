package se.arbetsprov.service.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings under {@code countries.*}.
 *
 * @param baseUri  the REST Countries v5 list endpoint
 * @param apiKey   REST Countries API key; blank means the bundled snapshot is used
 * @param pageSize countries per import call
 */
@ConfigurationProperties("countries")
public record CountriesProperties(
        @DefaultValue("https://api.restcountries.com/countries/v5") URI baseUri,
        String apiKey,
        @DefaultValue("100") int pageSize) {

    @Override
    public String toString() {
        // Never print the key, even if the properties end up in a log.
        return "CountriesProperties[baseUri=" + baseUri + ", apiKey=" + (apiKey == null || apiKey.isBlank() ? "none" : "****")
                + ", pageSize=" + pageSize + "]";
    }
}
