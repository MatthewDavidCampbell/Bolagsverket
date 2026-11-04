package se.arbetsprov.visits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static se.arbetsprov.visits.testing.VisitsFixture.T10;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.visits.testing.VisitsFixture;

/**
 * User story 1: mark a country as visited, with an optional note.
 * Country directory knows SWE, NOR, FIN and JPN (among others); the clock is fixed.
 */
@DisplayName("Create visit")
class VisitCreateTests {

    private final VisitsFixture fixture = new VisitsFixture();
    private final VisitsClient client = fixture.client;

    @Test
    void create_withNote_storesVisitWithNoteAndCreationTime() {
        // Given user "u1" with no visits and a clock fixed at 2026-11-04T10:00:00Z
        // When "SWE" is marked visited with note "Midsummer in Dalarna"
        var created = client.create("u1", "SWE", "Midsummer in Dalarna");

        // Then the returned and stored visit has user "u1", code "SWE", that note,
        // and created and updated times of 2026-11-04T10:00:00Z
        var expected = new Visit("u1", "SWE", "Midsummer in Dalarna", T10, T10);
        assertThat(created).isEqualTo(expected);
        assertThat(client.get("u1", "SWE")).contains(expected);
    }

    @Test
    void create_withLowerCaseCode_storesUpperCaseCode() {
        // Given user "u1"
        // When "swe" is marked visited
        var created = client.create("u1", "swe", null);

        // Then the visit's code is "SWE" and it can be read back as "SWE" or "swe"
        assertThat(created.countryCode()).isEqualTo("SWE");
        assertThat(client.get("u1", "SWE")).isPresent();
        assertThat(client.get("u1", "swe")).isPresent();
    }

    @Test
    void create_withoutNote_storesVisitWithNoNote() {
        // Given user "u1"
        // When "NOR" is marked visited with a null note, and "FIN" with note "   "
        client.create("u1", "NOR", null);
        client.create("u1", "FIN", "   ");

        // Then both visits have no note (null)
        assertThat(client.get("u1", "NOR")).hasValueSatisfying(v -> assertThat(v.note()).isNull());
        assertThat(client.get("u1", "FIN")).hasValueSatisfying(v -> assertThat(v.note()).isNull());
    }

    @Test
    void create_whenAlreadyVisited_isRejectedAndKeepsExistingVisit() {
        // Given "u1" visited "SWE" with note "first"
        client.create("u1", "SWE", "first");

        // When "SWE" is marked again with note "second"
        // Then a VisitAlreadyExistsException is thrown and the stored note is still "first"
        assertThatThrownBy(() -> client.create("u1", "SWE", "second"))
                .isInstanceOf(VisitAlreadyExistsException.class);
        assertThat(client.get("u1", "SWE")).hasValueSatisfying(v -> assertThat(v.note()).isEqualTo("first"));
    }

    @Test
    void create_unknownCountry_isRejected() {
        // Given a directory that does not know "XYZ"
        // When "u1" marks "XYZ" visited
        // Then an UnknownCountryException naming "XYZ" is thrown and nothing is stored
        assertThatThrownBy(() -> client.create("u1", "XYZ", null))
                .isInstanceOf(UnknownCountryException.class)
                .hasMessageContaining("XYZ");
        assertThat(client.list("u1")).isEmpty();
    }

    @Test
    void create_noteLongerThan500Characters_isRejected() {
        // Given a note of 501 characters (after trimming)
        var tooLong = "a".repeat(501);

        // When it is used to mark a visit
        // Then an IllegalArgumentException naming "note" is thrown; a note of exactly 500 is accepted
        assertThatThrownBy(() -> client.create("u1", "SWE", tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("note");
        assertThat(client.create("u1", "SWE", " " + "a".repeat(500) + " ").note()).hasSize(500);
    }

    @Test
    void create_noteWithSurroundingSpaces_isTrimmed() {
        // Given note "  Fika in Uppsala  "
        // When a visit is marked with it
        var created = client.create("u1", "SWE", "  Fika in Uppsala  ");

        // Then the stored note is "Fika in Uppsala"
        assertThat(created.note()).isEqualTo("Fika in Uppsala");
    }

    @Test
    void create_invalidUserId_isRejected() {
        // Given a blank user id and a user id of 65 characters
        // When each marks a visit
        // Then each is rejected with an IllegalArgumentException naming "userId"
        assertThatThrownBy(() -> client.create(" ", "SWE", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("userId");
        assertThatThrownBy(() -> client.create("u".repeat(65), "SWE", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("userId");
    }
}
