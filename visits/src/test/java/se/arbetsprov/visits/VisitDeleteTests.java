package se.arbetsprov.visits;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.visits.testing.VisitsFixture;

/**
 * User story 4: unmark a country.
 */
@DisplayName("Delete visit")
class VisitDeleteTests {

    private final VisitsClient client = new VisitsFixture().client;

    @Test
    void delete_visited_removesVisitAndReportsRemoved() {
        // Given "u1" visited SWE and NOR
        client.create("u1", "SWE", null);
        client.create("u1", "NOR", null);

        // When SWE is unmarked
        var removed = client.delete("u1", "SWE");

        // Then the call reports true and "u1"'s list holds only NOR
        assertThat(removed).isTrue();
        assertThat(client.list("u1")).extracting(Visit::countryCode).containsExactly("NOR");
    }

    @Test
    void delete_notVisited_changesNothingAndReportsNotRemoved() {
        // Given "u1" visited NOR only
        client.create("u1", "NOR", null);

        // When SWE is unmarked
        var removed = client.delete("u1", "SWE");

        // Then the call reports false and NOR is still listed
        assertThat(removed).isFalse();
        assertThat(client.list("u1")).extracting(Visit::countryCode).containsExactly("NOR");
    }

    @Test
    void delete_thenCreateAgain_isAllowed() {
        // Given "u1" visited SWE and then unmarked it
        client.create("u1", "SWE", "first trip");
        client.delete("u1", "SWE");

        // When SWE is marked again with a new note
        client.create("u1", "SWE", "second trip");

        // Then the new visit is stored with the new note
        assertThat(client.get("u1", "SWE")).hasValueSatisfying(v -> assertThat(v.note()).isEqualTo("second trip"));
    }
}
