package se.arbetsprov.service.countries;

import io.swagger.v3.oas.annotations.media.Schema;
import se.arbetsprov.countries.Country;

/** A country as returned by the API. */
@Schema(name = "Country")
public record CountryResponse(
        @Schema(description = "ISO 3166-1 alpha-3 code", example = "SWE") String code,
        @Schema(example = "Sweden") String name,
        @Schema(example = "Kingdom of Sweden") String officialName,
        @Schema(example = "Europe") String region,
        @Schema(example = "Northern Europe", nullable = true) String subregion,
        @Schema(example = "Stockholm", nullable = true) String capital,
        @Schema(example = "10621103") long population) {

    public static CountryResponse from(Country country) {
        return new CountryResponse(country.code(), country.name(), country.officialName(), country.region(),
                country.subregion(), country.capital(), country.population());
    }
}
