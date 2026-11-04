package se.arbetsprov.visits;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.arbetsprov.visits.testing.VisitsFixture;

/**
 * The cache is used from many virtual threads at once; operations are atomic per visit.
 */
@DisplayName("Visit concurrency")
class VisitConcurrencyTests {

    private static final List<String> TEN_COUNTRIES =
            List.of("SWE", "NOR", "FIN", "JPN", "DNK", "ISL", "DEU", "FRA", "ESP", "ITA");

    private final VisitsClient client = new VisitsFixture().client;

    @Test
    void create_sameVisitFromManyThreads_exactlyOneSucceeds() {
        // Given 100 virtual threads that all mark SWE for "u1" at the same time
        var start = new CountDownLatch(1);
        var succeeded = new AtomicInteger();
        var rejected = new AtomicInteger();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 100; i++) {
                executor.submit(() -> {
                    start.await();
                    try {
                        client.create("u1", "SWE", null);
                        succeeded.incrementAndGet();
                    } catch (VisitAlreadyExistsException e) {
                        rejected.incrementAndGet();
                    }
                    return null;
                });
            }
            start.countDown();
        }

        // When they finish
        // Then exactly one succeeded, 99 got VisitAlreadyExistsException, and "u1" has one visit
        assertThat(succeeded).hasValue(1);
        assertThat(rejected).hasValue(99);
        assertThat(client.list("u1")).hasSize(1);
    }

    @Test
    void create_manyUsersInParallel_noVisitIsLost() {
        // Given 1,000 users, each on its own virtual thread, marking the same 10 countries
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int u = 0; u < 1_000; u++) {
                String user = "user-" + u;
                executor.submit(() -> TEN_COUNTRIES.forEach(code -> client.create(user, code, "note " + code)));
            }
        }

        // When they finish
        // Then every user lists exactly those 10 visits (10,000 in total)
        int total = 0;
        for (int u = 0; u < 1_000; u++) {
            var codes = client.list("user-" + u).stream().map(Visit::countryCode).toList();
            assertThat(codes).containsExactlyInAnyOrderElementsOf(TEN_COUNTRIES);
            total += codes.size();
        }
        assertThat(total).isEqualTo(10_000);
    }

    @Test
    void options_backend_defaultsToInProcess() {
        // Given default visits client options
        // When the backend is read
        // Then it is IN_PROCESS, the only backend implemented
        assertThat(new VisitsClientOptions().backend()).isEqualTo(VisitsBackend.IN_PROCESS);
        assertThat(VisitsBackend.values()).containsExactly(VisitsBackend.IN_PROCESS);
    }
}
