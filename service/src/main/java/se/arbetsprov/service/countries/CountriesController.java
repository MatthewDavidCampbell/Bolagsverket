package se.arbetsprov.service.countries;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.arbetsprov.countries.CountriesClient;
import se.arbetsprov.countries.CountryQuery;
import se.arbetsprov.countries.CountrySort;
import se.arbetsprov.countries.SortDirection;

/** Countries, read from the local cache only (brief, user story 2). */
@RestController
@RequestMapping("/api/countries")
@Tag(name = "Countries", description = "Countries imported at start-up; never read from REST Countries")
public class CountriesController {

    private final CountriesClient countries;

    public CountriesController(CountriesClient countries) {
        this.countries = countries;
    }

    @GetMapping
    @Operation(summary = "List countries", description = "Paged, optionally filtered by region, sorted by name or population.")
    @ApiResponse(responseCode = "200", description = "One page of countries")
    @ApiResponse(responseCode = "400", description = "Invalid page, size, sort or direction",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<CountryResponse> list(
            @Parameter(description = "Region, ignoring letter case", example = "Europe")
            @RequestParam(required = false) String region,
            @Parameter(description = "Sort field (letter case ignored)")
            @RequestParam(defaultValue = "NAME") CountrySort sort,
            @Parameter(description = "Sort direction (letter case ignored)")
            @RequestParam(defaultValue = "ASC") SortDirection direction,
            @Parameter(description = "Page number, from 0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, 1 to 100")
            @RequestParam(defaultValue = "20") int size) {
        var query = new CountryQuery(region, sort, direction, page, size);
        return PageResponse.from(countries.list(query), CountryResponse::from);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get one country by ISO alpha-3 code")
    @ApiResponse(responseCode = "200", description = "The country")
    @ApiResponse(responseCode = "404", description = "Unknown country code",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public CountryResponse get(@Parameter(example = "SWE") @PathVariable String code) {
        return countries.get(code)
                .map(CountryResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown country code: " + code));
    }
}
