package se.arbetsprov.visits;

import java.time.Clock;
import java.util.Objects;
import se.arbetsprov.core.client.ClientOptions;

/**
 * Settings for {@link VisitsClient}. The backend defaults to {@link VisitsBackend#IN_PROCESS},
 * which needs no base URI; the inherited remote settings (base URI, credential, transport,
 * policies) are where a remote visits service would be configured.
 */
public final class VisitsClientOptions extends ClientOptions {

    private VisitsBackend backend = VisitsBackend.IN_PROCESS;
    private Clock clock = Clock.systemUTC();

    /** Where visits are kept. */
    public VisitsClientOptions backend(VisitsBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
        return this;
    }

    public VisitsBackend backend() {
        return backend;
    }

    /** The clock for created and updated times; replaceable in tests. */
    public VisitsClientOptions clock(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
        return this;
    }

    public Clock clock() {
        return clock;
    }
}
