package se.arbetsprov.core.testing;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import se.arbetsprov.core.http.HttpClient;
import se.arbetsprov.core.http.HttpRequest;
import se.arbetsprov.core.http.HttpResponse;

/** A transport that records requests and answers with a given function. */
public final class FakeHttpClient implements HttpClient {

    private final Function<HttpRequest, HttpResponse> responder;
    private final List<HttpRequest> requests = new CopyOnWriteArrayList<>();
    private final List<String> log;

    private FakeHttpClient(Function<HttpRequest, HttpResponse> responder, List<String> log) {
        this.responder = responder;
        this.log = log;
    }

    /** Answers every request with {@code status} and no body. */
    public static FakeHttpClient returning(int status) {
        return new FakeHttpClient(r -> new HttpResponse(r, status, null, null), null);
    }

    /** Answers with the given function. */
    public static FakeHttpClient answering(Function<HttpRequest, HttpResponse> responder) {
        return new FakeHttpClient(responder, null);
    }

    /** Answers {@code status} and appends "transport" to a shared log. */
    public static FakeHttpClient logging(int status, List<String> log) {
        return new FakeHttpClient(r -> new HttpResponse(r, status, null, null), log);
    }

    @Override
    public HttpResponse send(HttpRequest request) {
        requests.add(request);
        if (log != null) {
            log.add("transport");
        }
        return responder.apply(request);
    }

    /** All requests received, in order. */
    public List<HttpRequest> requests() {
        return requests;
    }
}
