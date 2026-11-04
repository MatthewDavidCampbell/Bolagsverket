package se.arbetsprov.service.visits;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import se.arbetsprov.countries.CountriesClient;
import se.arbetsprov.countries.Country;
import se.arbetsprov.visits.Visit;
import se.arbetsprov.visits.VisitsClient;

/**
 * A user's visited countries (brief, user story 3). The user comes from the {@code X-User-Id}
 * header, standing in for an identity from an authentication token. Responses add the country
 * name from the countries cache: the API composes the two service libraries.
 */
@RestController
@RequestMapping("/api/visits")
@Tag(name = "Visits", description = "Countries the calling user has visited, with a short note")
public class VisitsController {

    static final String USER_HEADER = "X-User-Id";

    private final VisitsClient visits;
    private final CountriesClient countries;

    public VisitsController(VisitsClient visits, CountriesClient countries) {
        this.visits = visits;
        this.countries = countries;
    }

    @GetMapping
    @Operation(summary = "List my visited countries", description = "Ordered by country code, with notes.")
    @ApiResponse(responseCode = "200", description = "The user's visits")
    @ApiResponse(responseCode = "400", description = "Missing or invalid X-User-Id", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public List<VisitResponse> list(@UserId @RequestHeader(USER_HEADER) String userId) {
        return visits.list(userId).stream().map(this::toResponse).toList();
    }

    @PostMapping
    @Operation(summary = "Mark a country as visited")
    @ApiResponse(responseCode = "201", description = "Marked; Location points at the new visit")
    @ApiResponse(responseCode = "400", description = "Unknown country, note too long, or missing X-User-Id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Already marked as visited", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<VisitResponse> create(
            @UserId @RequestHeader(USER_HEADER) String userId,
            @RequestBody VisitRequest request) {
        Visit visit = visits.create(userId, request.countryCode(), request.note());
        var location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{code}").buildAndExpand(visit.countryCode()).toUri();
        return ResponseEntity.created(location).body(toResponse(visit));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get my visit to one country")
    @ApiResponse(responseCode = "200", description = "The visit")
    @ApiResponse(responseCode = "404", description = "Not marked as visited", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public VisitResponse get(
            @UserId @RequestHeader(USER_HEADER) String userId,
            @Parameter(example = "SWE") @PathVariable String code) {
        return visits.get(userId, code)
                .map(this::toResponse)
                .orElseThrow(() -> notVisited(code));
    }

    @PutMapping("/{code}")
    @Operation(summary = "Change the note of my visit", description = "An empty or null note removes it.")
    @ApiResponse(responseCode = "200", description = "The updated visit")
    @ApiResponse(responseCode = "400", description = "Note too long or missing X-User-Id", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Not marked as visited", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public VisitResponse updateNote(
            @UserId @RequestHeader(USER_HEADER) String userId,
            @Parameter(example = "SWE") @PathVariable String code,
            @RequestBody NoteRequest request) {
        return toResponse(visits.updateNote(userId, code, request.note()));
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Unmark a country as visited")
    @ApiResponse(responseCode = "204", description = "Unmarked")
    @ApiResponse(responseCode = "404", description = "Not marked as visited", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> delete(
            @UserId @RequestHeader(USER_HEADER) String userId,
            @Parameter(example = "SWE") @PathVariable String code) {
        if (!visits.delete(userId, code)) {
            throw notVisited(code);
        }
        return ResponseEntity.noContent().build();
    }

    private VisitResponse toResponse(Visit visit) {
        String name = countries.get(visit.countryCode()).map(Country::name).orElse(null);
        return VisitResponse.from(visit, name);
    }

    private static ResponseStatusException notVisited(String code) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Country " + code.strip().toUpperCase(Locale.ROOT) + " is not marked as visited");
    }
}
