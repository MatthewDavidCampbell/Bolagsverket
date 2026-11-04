package se.arbetsprov.core.http.policy;

import se.arbetsprov.core.http.HttpPipelineNextPolicy;
import se.arbetsprov.core.http.HttpRequest;
import se.arbetsprov.core.http.HttpResponse;

/**
 * One step in the pipeline. A policy sees every request on the way down and every response on
 * the way back up, so it can add headers, repeat calls, log or short-circuit.
 *
 * <p>Policies must not keep per-call state in fields: one pipeline serves many concurrent
 * callers.
 */
@FunctionalInterface
public interface HttpPipelinePolicy {

    /**
     * Handles one call.
     *
     * @param request the request, possibly already changed by earlier policies
     * @param next    the rest of the pipeline; call it to pass the request on
     * @return the response, possibly changed on the way up
     */
    HttpResponse process(HttpRequest request, HttpPipelineNextPolicy next);
}
