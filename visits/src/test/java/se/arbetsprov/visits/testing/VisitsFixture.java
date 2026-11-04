package se.arbetsprov.visits.testing;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import se.arbetsprov.visits.VisitsClient;
import se.arbetsprov.visits.VisitsClientOptions;

/** A visits client with a fixed country directory (SWE, NOR, FIN, JPN) and a settable clock. */
public final class VisitsFixture {

    public static final Instant T10 = Instant.parse("2026-11-04T10:00:00Z");
    public static final Instant T11 = Instant.parse("2026-11-04T11:00:00Z");

    public final SettableClock clock = new SettableClock(T10);
    public final VisitsClient client = new VisitsClient(
            new VisitsClientOptions().clock(clock),
            Set.of("SWE", "NOR", "FIN", "JPN", "DNK", "ISL", "DEU", "FRA", "ESP", "ITA")::contains);

    /** A clock that stays at one instant until moved. */
    public static final class SettableClock extends Clock {

        private volatile Instant now;

        public SettableClock(Instant now) {
            this.now = now;
        }

        public void set(Instant now) {
            this.now = now;
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
