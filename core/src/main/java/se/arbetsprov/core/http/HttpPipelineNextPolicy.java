package se.arbetsprov.core.http;

/**
 * The rest of the pipeline as seen from one policy: the following policies and, last, the
 * transport.
 */
@FunctionalInterface
public interface HttpPipelineNextPolicy {

    /** Passes the request on and returns the response from further down. */
    HttpResponse process(HttpRequest request);
}
