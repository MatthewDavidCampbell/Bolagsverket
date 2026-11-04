package se.arbetsprov.visits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.visits.testing.VisitsFixture;

/**
 * User story 2: list my visited countries with notes.
 */
@DisplayName("List visits")
class VisitListTests {

    private final VisitsClient client = new VisitsFixture().client;

    @Test
    void list_returnsOwnVisitsWithNotesOrderedByCode() {
        // Given "u1" visited SWE (note "Stockholm") and then NOR (no note)
        client.create("u1", "SWE", "Stockholm");
        client.create("u1", "NOR", null);

        // When "u1" lists visits
        var visits = client.list("u1");

        // Then NOR and SWE are returned in that order, with their notes
        assertThat(visits).extracting(Visit::countryCode, Visit::note)
                .containsExactly(tuple("NOR", null), tuple("SWE", "Stockholm"));
    }

    @Test
    void list_userWithNoVisits_returnsEmptyList() {
        // Given a user who has never marked a country
        // When they list visits
        // Then the list is empty
        assertThat(client.list("newcomer")).isEmpty();
    }

    @Test
    void list_doesNotIncludeOtherUsersVisits() {
        // Given "u1" visited SWE and "u2" visited JPN
        client.create("u1", "SWE", null);
        client.create("u2", "JPN", null);

        // When "u1" lists visits
        // Then only SWE is returned
        assertThat(client.list("u1")).extracting(Visit::countryCode).containsExactly("SWE");
    }
}
