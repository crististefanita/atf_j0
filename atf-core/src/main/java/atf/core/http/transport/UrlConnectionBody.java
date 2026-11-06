package atf.core.http.transport;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/** One operation owns the subscription, including subscriptions delivered after cancellation. */
final class UrlConnectionBody implements Flow.Subscriber<ByteBuffer> {
    private final CompletableFuture<byte[]> bytes = new CompletableFuture<>();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private Flow.Subscription subscription;
    private boolean finished;
    private Throwable stopped;

    CompletableFuture<byte[]> collect(HttpRequest.BodyPublisher publisher) {
        synchronized (this) { if (stopped != null) return bytes; }
        publisher.subscribe(this);
        return bytes;
    }

    @Override public void onSubscribe(Flow.Subscription value) {
        Throwable terminal;
        synchronized (this) {
            terminal = stopped;
            if (terminal == null && subscription == null) subscription = value;
            else if (terminal == null) terminal = new IllegalStateException("Duplicate body subscription");
        }
        if (terminal == null) value.request(Long.MAX_VALUE);
        else cancel(value, terminal);
    }

    @Override public synchronized void onNext(ByteBuffer buffer) {
        if (finished || stopped != null) return;
        var copy = buffer.slice();
        var chunk = new byte[copy.remaining()]; copy.get(chunk); output.writeBytes(chunk);
    }

    @Override public synchronized void onError(Throwable failure) {
        if (finished || stopped != null) return;
        finished = true; bytes.completeExceptionally(failure);
    }

    @Override public synchronized void onComplete() {
        if (finished || stopped != null) return;
        finished = true; bytes.complete(output.toByteArray());
    }

    void stop(Throwable terminal) {
        Flow.Subscription acquired;
        synchronized (this) {
            if (stopped != null) return;
            stopped = terminal; acquired = subscription; subscription = null;
            bytes.completeExceptionally(terminal);
        }
        if (acquired != null) cancel(acquired, terminal);
    }

    private static void cancel(Flow.Subscription value, Throwable terminal) {
        try { value.cancel(); }
        catch (RuntimeException | Error cleanup) { if (cleanup != terminal) terminal.addSuppressed(cleanup); }
    }
}
