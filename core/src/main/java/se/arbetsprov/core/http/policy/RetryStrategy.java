package se.arbetsprov.core.http.policy;

/**
 * How a {@link RetryPolicy} would space repeated attempts. Defined to show the extension point;
 * no strategy is acted on yet.
 */
public enum RetryStrategy {

    /** Wait longer after each failed attempt (for example 0.8 s, 1.6 s, 3.2 s), with jitter. */
    EXPONENTIAL_BACKOFF,

    /** Wait the same delay between every attempt. */
    FIXED_DELAY
}
