package se.arbetsprov.service.visits;

import io.swagger.v3.oas.annotations.media.Schema;

/** Body for changing the note of a visit. */
@Schema(name = "NoteRequest")
public record NoteRequest(
        @Schema(description = "New note, at most 500 characters; empty or null removes it",
                example = "Fika in Uppsala", nullable = true) String note) {
}
