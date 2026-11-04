package se.arbetsprov.service.visits;

import io.swagger.v3.oas.annotations.media.Schema;

/** Body for marking a country as visited. */
@Schema(name = "VisitRequest")
public record VisitRequest(
        @Schema(description = "ISO alpha-3 code, letter case ignored", example = "SWE",
                requiredMode = Schema.RequiredMode.REQUIRED) String countryCode,
        @Schema(description = "Optional short note, at most 500 characters", example = "Midsummer in Dalarna",
                nullable = true) String note) {
}
