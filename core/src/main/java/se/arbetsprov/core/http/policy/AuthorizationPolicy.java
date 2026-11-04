package se.arbetsprov.core.http.policy;

import se.arbetsprov.core.http.HttpPipelineNextPolicy;
import se.arbetsprov.core.http.HttpRequest;
import se.arbetsprov.core.http.HttpResponse;

/**
 * Skeleton: marks where service-to-service credentials would be attached. Today it passes every
 * request through unchanged; the only remote service in use (restcountries) is public.
 *
 * <p>A full version would take a credential source (for example an OAuth 2.0 client-credentials
 * token provider), cache the token until shortly before it expires, and add
 * {@code Authorization: Bearer <token>} to each request.
 */
public final class AuthorizationPolicy implements HttpPipelinePolicy {

    @Override
    public HttpResponse process(HttpRequest request, HttpPipelineNextPolicy next) {
        return next.process(request);
    }
}
