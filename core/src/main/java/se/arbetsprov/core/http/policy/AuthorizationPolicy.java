package se.arbetsprov.core.http.policy;

import se.arbetsprov.core.credential.KeyCredential;
import se.arbetsprov.core.http.HttpPipelineNextPolicy;
import se.arbetsprov.core.http.HttpRequest;
import se.arbetsprov.core.http.HttpResponse;

/**
 * Adds {@code Authorization: Bearer <key>} to every request. Without a credential the request
 * passes through unchanged, which suits public services and local stand-ins.
 *
 * <p>An API key is the simplest credential. Between internal services the same policy would take
 * a token provider instead (for example OAuth 2.0 client credentials) and cache the token until
 * shortly before it expires; callers and other policies would not change.
 */
public final class AuthorizationPolicy implements HttpPipelinePolicy {

    private final KeyCredential credential;

    /** A policy with no credential: requests pass through unchanged. */
    public AuthorizationPolicy() {
        this(null);
    }

    /** A policy that sends {@code credential} as a bearer token; null means no credential. */
    public AuthorizationPolicy(KeyCredential credential) {
        this.credential = credential;
    }

    @Override
    public HttpResponse process(HttpRequest request, HttpPipelineNextPolicy next) {
        if (credential == null) {
            return next.process(request);
        }
        return next.process(request.withHeader("Authorization", "Bearer " + credential.key()));
    }
}
