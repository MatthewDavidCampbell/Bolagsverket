package se.arbetsprov.core.testing;

import java.util.List;
import se.arbetsprov.core.http.HttpPipelineNextPolicy;
import se.arbetsprov.core.http.HttpRequest;
import se.arbetsprov.core.http.HttpResponse;
import se.arbetsprov.core.http.policy.HttpPipelinePolicy;

/** A pass-through policy that appends "name-request" and "name-response" to a shared log. */
public record RecordingPolicy(String name, List<String> log) implements HttpPipelinePolicy {

    @Override
    public HttpResponse process(HttpRequest request, HttpPipelineNextPolicy next) {
        log.add(name + "-request");
        HttpResponse response = next.process(request);
        log.add(name + "-response");
        return response;
    }
}
