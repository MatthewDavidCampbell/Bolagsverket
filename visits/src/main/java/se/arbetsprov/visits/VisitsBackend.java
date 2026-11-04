package se.arbetsprov.visits;

/**
 * Where {@link VisitsClient} keeps visits.
 *
 * <p>Only {@link #IN_PROCESS} exists in this sample. When visits grows into a service of its own,
 * a {@code REMOTE} value would select a backend that calls it over REST through the core
 * pipeline (base URI and credential from {@link VisitsClientOptions}); callers would not change.
 */
public enum VisitsBackend {

    /** An in-memory cache inside the application. Lost on restart; one copy per replica. */
    IN_PROCESS
}
