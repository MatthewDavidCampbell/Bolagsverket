package se.arbetsprov.visits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static se.arbetsprov.visits.testing.VisitsFixture.T10;
import static se.arbetsprov.visits.testing.VisitsFixture.T11;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.visits.testing.VisitsFixture;

/**
 * User story 3: read and change the note of one visit.
 */
@DisplayName("Read and update visit")
class VisitUpdateTests {

    private final VisitsFixture fixture = new VisitsFixture();
    private final VisitsClient client = fixture.client;

    @Test
    void get_visited_returnsVisit() {
        // Given "u1" visited SWE
        client.create("u1", "SWE", "Stockholm");

        // When "u1" reads SWE
        // Then the visit is returned
        assertThat(client.get("u1", "SWE")).hasValueSatisfying(v -> assertThat(v.note()).isEqualTo("Stockholm"));
    }

    @Test
    void get_notVisited_returnsEmpty() {
        // Given "u1" has not visited FIN
        // When "u1" reads FIN
        // Then nothing is returned
        assertThat(client.get("u1", "FIN")).isEmpty();
    }

    @Test
    void updateNote_replacesNoteKeepsCreatedAtAndMovesUpdatedAt() {
        // Given "u1" visited SWE with note "old" at 10:00, and the clock now at 11:00
        client.create("u1", "SWE", "old");
        fixture.clock.set(T11);

        // When the note is replaced with "new"
        var updated = client.updateNote("u1", "SWE", "new");

        // Then the note is "new", created at is still 10:00 and updated at is 11:00
        assertThat(updated.note()).isEqualTo("new");
        assertThat(updated.createdAt()).isEqualTo(T10);
        assertThat(updated.updatedAt()).isEqualTo(T11);
        assertThat(client.get("u1", "SWE")).contains(updated);
    }

    @Test
    void updateNote_withBlankNote_removesNote() {
        // Given "u1" visited SWE with note "old"
        client.create("u1", "SWE", "old");

        // When the note is replaced with ""
        client.updateNote("u1", "SWE", "");

        // Then the visit has no note
        assertThat(client.get("u1", "SWE")).hasValueSatisfying(v -> assertThat(v.note()).isNull());
    }

    @Test
    void updateNote_notVisited_throwsNotFound() {
        // Given "u1" has not visited FIN
        // When "u1" updates the note for FIN
        // Then a VisitNotFoundException is thrown
        assertThatThrownBy(() -> client.updateNote("u1", "FIN", "note"))
                .isInstanceOf(VisitNotFoundException.class);
    }

    @Test
    void updateNote_otherUsersVisit_isNotFoundAndLeavesItUnchanged() {
        // Given "u2" visited JPN with note "Kyoto"
        client.create("u2", "JPN", "Kyoto");

        // When "u1" updates the note for JPN
        // Then a VisitNotFoundException is thrown and "u2"'s note is still "Kyoto"
        assertThatThrownBy(() -> client.updateNote("u1", "JPN", "mine now"))
                .isInstanceOf(VisitNotFoundException.class);
        assertThat(client.get("u2", "JPN")).hasValueSatisfying(v -> assertThat(v.note()).isEqualTo("Kyoto"));
    }
}
