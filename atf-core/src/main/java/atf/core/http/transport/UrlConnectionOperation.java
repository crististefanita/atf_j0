package atf.core.http.transport;

import atf.core.http.auth.ApiAuthenticator;
import atf.core.http.protocol.HttpHeaderProfiles;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.concurrent.*;

/** Owns one blocking exchange; a terminal body cannot acquire a later dispatch. */
final class UrlConnectionOperation implements Runnable {
    private final Object gate = new Object();
    private final ApiAuthenticator authenticator;
    private final ApiRequest request;
    private final String url;
    private final String authProfile;
    private final long started = System.nanoTime();
    private final long timeoutNanos;
    private final UrlConnectionBody body = new UrlConnectionBody();
    private final CompletableFuture<Void> expiry = new CompletableFuture<>();
    private final Thread worker = Thread.ofVirtual().unstarted(this);
    private final CompletableFuture<ApiResponse> result = new CompletableFuture<>() {
        @Override public boolean cancel(boolean interrupt) {
            return finish(null, new CancellationException("URL connection cancelled"), interrupt) || isCancelled();
        }
    };
    private boolean terminal;
    private HttpURLConnection connection;

    UrlConnectionOperation(ApiAuthenticator authenticator, Duration timeout, ApiRequest request, String url, String authProfile) {
        this.authenticator = authenticator; this.timeoutNanos = timeout.toNanos();
        this.request = request; this.url = url; this.authProfile = authProfile;
    }

    CompletableFuture<ApiResponse> start() {
        long remaining = Math.max(0, timeoutNanos - (System.nanoTime() - started));
        expiry.orTimeout(remaining, TimeUnit.NANOSECONDS).whenComplete((value, failure) -> {
            if (failure instanceof TimeoutException) Thread.ofPlatform().daemon(true)
                    .name("atf-url-connection-deadline").start(() -> finish(null, failure, true));
        });
        worker.start();
        return result;
    }

    @Override public void run() {
        try { finish(execute(), null, false); }
        catch (Throwable failure) {
            if (failure instanceof ExecutionException && failure.getCause() != null) failure = failure.getCause();
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            finish(null, failure, true);
        }
    }

    private ApiResponse execute() throws Exception {
        requireActive();
        byte[] payload = body.collect(request.body()).get(remainingNanos(), TimeUnit.NANOSECONDS);
        var acquired = (HttpURLConnection) URI.create(url).toURL().openConnection();
        acquired.setRequestMethod(request.method()); acquired.setDoInput(true);
        HttpHeaderProfiles.JSON_REQUEST.forEach(acquired::setRequestProperty);
        request.headers().forEach(acquired::setRequestProperty);
        var auth = HttpRequest.newBuilder(URI.create(url)); authenticator.apply(auth, authProfile);
        auth.build().headers().map().forEach((name, values) -> acquired.setRequestProperty(name, String.join(", ", values)));
        if (payload.length > 0) { acquired.setDoOutput(true); acquired.setFixedLengthStreamingMode(payload.length); }
        synchronized (gate) {
            requireActive(); remainingNanos(); connection = acquired;
        }
        // The acquisition above is the dispatch boundary; later cancellation cannot establish rollback.
        applyRemainingTimeout(acquired);
        if (payload.length > 0) {
            try (var output = acquired.getOutputStream()) { output.write(payload); }
        }
        applyRemainingTimeout(acquired);
        int status = acquired.getResponseCode();
        var response = new ByteArrayOutputStream();
        try (var input = status >= 400 ? acquired.getErrorStream() : acquired.getInputStream()) {
            if (input != null) {
                var chunk = new byte[8192];
                while (true) {
                    applyRemainingTimeout(acquired);
                    int count = input.read(chunk); if (count < 0) break; response.write(chunk, 0, count);
                }
            }
        }
        var headers = new LinkedHashMap<String, String>();
        acquired.getHeaderFields().forEach((name, values) -> { if (name != null && values != null) headers.put(name, String.join(", ", values)); });
        return new ApiResponse(status, response.toString(StandardCharsets.UTF_8), headers,
                Duration.ofNanos(System.nanoTime() - started), url);
    }

    private void requireActive() throws TimeoutException {
        synchronized (gate) { if (terminal) throw new CancellationException("URL connection already terminated"); }
        remainingNanos();
    }

    private long remainingNanos() throws TimeoutException {
        long remaining = timeoutNanos - (System.nanoTime() - started);
        if (remaining <= 0) throw new TimeoutException("URL connection deadline exceeded");
        return remaining;
    }

    private void applyRemainingTimeout(HttpURLConnection acquired) throws TimeoutException {
        requireActive();
        int millis = (int) Math.min(Integer.MAX_VALUE, 1 + (remainingNanos() - 1) / 1_000_000);
        acquired.setConnectTimeout(millis); acquired.setReadTimeout(millis);
    }

    private boolean finish(ApiResponse response, Throwable failure, boolean interrupt) {
        HttpURLConnection acquired;
        synchronized (gate) {
            if (terminal) return false;
            if (failure == null && System.nanoTime() - started >= timeoutNanos)
                failure = new TimeoutException("URL connection deadline exceeded");
            terminal = true; acquired = connection; connection = null;
        }
        expiry.complete(null);
        if (failure != null) {
            body.stop(failure);
            if (interrupt && worker != Thread.currentThread()) worker.interrupt();
        }
        if (acquired != null) {
            try { acquired.disconnect(); }
            catch (RuntimeException | Error cleanup) {
                if (failure == null) failure = cleanup;
                else if (failure != cleanup) failure.addSuppressed(cleanup);
            }
        }
        if (failure == null) result.complete(response);
        else result.completeExceptionally(failure);
        return true;
    }
}
