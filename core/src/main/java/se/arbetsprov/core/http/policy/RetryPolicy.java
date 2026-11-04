package se.arbetsprov.core.http.policy;

import java.util.Objects;
import se.arbetsprov.core.http.HttpPipelineNextPolicy;
import se.arbetsprov.core.http.HttpRequest;
import se.arbetsprov.core.http.HttpResponse;

/**
 * Skeleton: marks where transient failures (connection errors, 408, 429, 5xx) would be retried
 * according to a {@link RetryStrategy}. Today it passes every request through once, unchanged.
 *
 * <p>A full version would call {@code next.process(request)} in a loop, wait between attempts
 * as the strategy says, honour a {@code Retry-After} header and give up after a maximum number
 * of attempts. Because each pass through {@code next} reaches the later policies again, placing
 * {@link AuthorizationPolicy} after this policy gives every attempt a fresh credential.
 */
public final class RetryPolicy implements HttpPipelinePolicy {

    private final RetryStrategy strategy;

    public RetryPolicy(RetryStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    /** The strategy this policy would use. */
    public RetryStrategy strategy() {
        return strategy;
    }

    @Override
    public HttpResponse process(HttpRequest request, HttpPipelineNextPolicy next) {
        return next.process(request);
    }
}
