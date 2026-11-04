package se.arbetsprov.service.visits;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import se.arbetsprov.visits.Visit;

/** A visited country with its note, enriched with the country name from the countries cache. */
@Schema(name = "Visit")
public record VisitResponse(
        @Schema(example = "SWE") String countryCode,
        @Schema(example = "Sweden") String countryName,
        @Schema(example = "Midsummer in Dalarna", nullable = true) String note,
        Instant createdAt,
        Instant updatedAt) {

    public static VisitResponse from(Visit visit, String countryName) {
        return new VisitResponse(visit.countryCode(), countryName, visit.note(), visit.createdAt(), visit.updatedAt());
    }
}
