package atf.ui.network;

import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.network.delivery.UiNetworkDeliveryFailure;
import atf.ui.network.identity.UiRequestIdentity;
import atf.ui.network.identity.UiRequestTimeline;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.LongFunction;

/**
 * Generic Playwright boundary for bounded, typed API observations.
 *
 * <p>It is deliberately independent of Extent, business routes and test
 * assertions. Consumers can subscribe and adapt observations to their own
 * report contract; the inspector itself never creates report nodes or
 * decides a verdict.</p>
 */
public final class UiNetworkInspector implements AutoCloseable {
    private final UiNetworkInspectionConfiguration configuration;
    private final AtomicLong sequence = new AtomicLong();
    private final List<UiNetworkObservation> observations = new CopyOnWriteArrayList<>();
    private final List<Consumer<UiNetworkObservation>> subscribers = new CopyOnWriteArrayList<>();
    private final List<UiNetworkDeliveryFailure> deliveryFailures = new CopyOnWriteArrayList<>();
    private final UiRequestTimeline requestStarts = new UiRequestTimeline();
    private final Object captureLock = new Object();
    private long accepted;
    private long excluded;
    private long dropped;
    private long inFlight;
    private boolean limitReached;
    private Page page;
    private Consumer<Request> requestFinishedListener;
    private Consumer<Request> requestFailedListener;
    private boolean attaching;
    private boolean closed;

    public UiNetworkInspector(UiNetworkInspectionConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public UiNetworkInspectionConfiguration configuration() { return configuration; }

    public void attach(Page page) { attach(page, ignored -> { }); }

    public void attach(Page page, Consumer<UiNetworkObservation> subscriber) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(subscriber, "subscriber");
        if (!configuration.enabled()) return;
        synchronized (captureLock) {
            if (this.page != null || closed) throw new IllegalStateException("UI network inspector is already attached or closed");
            this.page = page;
            attaching = true;
            try {
                requestStarts.attach(page);
                requireOpenAttachment();
                requestFinishedListener = this::observeFinished;
                page.onRequestFinished(requestFinishedListener);
                requireOpenAttachment();
                requestFailedListener = this::observeFailure;
                page.onRequestFailed(requestFailedListener);
                requireOpenAttachment();
                subscribers.add(subscriber);
                attaching = false;
            } catch (RuntimeException | Error failure) {
                release(failure);
                throw failure;
            }
        }
    }

    /** Allows a consumer adapter to subscribe after the Playwright session opened. */
    public void subscribe(Consumer<UiNetworkObservation> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        if (!configuration.enabled()) return;
        synchronized (captureLock) {
            if (closed) throw new IllegalStateException("UI network inspector is closed");
            subscribers.add(subscriber);
        }
    }

    /** Component-test seam and a safe adapter entry point for custom capture. */
    public Optional<UiNetworkObservation> capture(Response response) {
        if (!configuration.enabled() || response == null || !acceptsCaptures()) return Optional.empty();
        Request request = response.request();
        if (request == null) return Optional.empty();
        return captureSelected(request.url(), UiNetworkCapturePolicy.ResourceKind.fromPlaywright(request.resourceType()),
                next -> started(UiNetworkObservation.from(response, next, configuration), request));
    }

    public UiRequestIdentity requestMarker() { return requestStarts.marker(); }
    public UiRequestIdentity requestStarted() { return requestStarts.begin(); }
    private UiNetworkObservation started(UiNetworkObservation observation, Request request) {
        return observation.withRequestIdentity(requestStarts.identity(request).orElse(null));
    }

    /**
     * Publishes an already typed observation through the same bounded path as
     * browser responses. Consumers and component fixtures use this seam when a
     * real Playwright response is intentionally outside the test boundary;
     * The inspector re-applies the configured redaction, bounding and
     * signature policy before storage or subscriber notification.
     */
    public Optional<UiNetworkObservation> capture(UiNetworkObservation observation) {
        if (observation == null) return Optional.empty();
        return captureSelected(observation.url(), observation.resourceKind(),
                ignored -> observation.sanitizedFor(configuration));
    }

    /** Scope-local counters; excluded traffic never consumes the bounded journal. */
    public UiNetworkCaptureStatistics statistics() {
        synchronized (captureLock) {
            return new UiNetworkCaptureStatistics(accepted, observations.size(), excluded,
                    dropped, inFlight, limitReached);
        }
    }

    /** Compatibility view; statistics() also exposes exclusions and pending reservations. */
    public CaptureSummary captureSummary() {
        var snapshot = statistics();
        return new CaptureSummary(configuration.capturePolicy().scope(),
                Math.toIntExact(snapshot.accepted()), Math.toIntExact(snapshot.retained()),
                Math.toIntExact(snapshot.dropped()), snapshot.limitReached());
    }

    public record CaptureSummary(String scope, int accepted, int retained, int dropped,
                                 boolean limitReached) { }

    private Optional<UiNetworkObservation> captureSelected(String url, UiNetworkCapturePolicy.ResourceKind type,
                                                          LongFunction<UiNetworkObservation> factory) {
        if (!configuration.enabled()) return Optional.empty();
        boolean selected;
        try {
            selected = configuration.accepts(url) && configuration.capturePolicy().accepts(url, type);
        } catch (IllegalArgumentException | IllegalStateException malformedUrl) {
            selected = false;
        }
        synchronized (captureLock) {
            if (closed || attaching) return Optional.empty();
            if (!selected) {
                excluded++;
                return Optional.empty();
            }
            accepted++;
            if (observations.size() + inFlight >= configuration.maxObservations()) {
                dropped++;
                limitReached = true;
                return Optional.empty();
            }
            inFlight++;
        }
        UiNetworkObservation observation;
        try {
            observation = Objects.requireNonNull(factory.apply(sequence.incrementAndGet()), "observation");
        } catch (RuntimeException | Error failure) {
            synchronized (captureLock) { inFlight--; dropped++; }
            throw failure;
        }
        List<Consumer<UiNetworkObservation>> delivery;
        synchronized (captureLock) {
            inFlight--;
            if (closed || attaching) { dropped++; return Optional.empty(); }
            observations.add(observation);
            delivery = List.copyOf(subscribers);
        }
        // Subscriber failures cannot turn a retained observation into a dropped one.
        notifySubscribers(observation, delivery);
        return Optional.of(observation);
    }

    /** Retained causes for this owned capture scope; raw retention counters are unchanged. */
    public List<UiNetworkDeliveryFailure> deliveryFailures() { return List.copyOf(deliveryFailures); }

    /** Returns lossless bounded observations in browser order. */
    public List<UiNetworkObservation> observations() {
        return List.copyOf(observations);
    }

    /** Capture this cursor before a UI action to exclude earlier read-backs. */
    public int observationCursor() {
        return observations.size();
    }

    /** An append-only window; presentation collapsing does not change cursor semantics. */
    public List<UiNetworkObservation> observationsSince(int cursor) {
        List<UiNetworkObservation> snapshot = observations();
        if (cursor < 0 || cursor > snapshot.size()) {
            throw new IllegalArgumentException("UI network observation cursor is outside this journal");
        }
        return List.copyOf(snapshot.subList(cursor, snapshot.size()));
    }

    /**
     * Returns the compact presentation stream while keeping raw observations
     * available for audit and signature counts. Consecutive identical
     * signatures are represented once when the configured policy enables it;
     * non-consecutive repetitions remain visible because they may belong to
     * different causal moments in the UI journey.
     */
    public List<UiNetworkObservation> presentationObservations() {
        if (!configuration.collapseConsecutiveSignatures()) return observations();
        List<UiNetworkObservation> compact = new ArrayList<>();
        String previousSignature = null;
        for (UiNetworkObservation observation : observations) {
            if (!Objects.equals(previousSignature, observation.requestSignature())) {
                compact.add(observation);
                previousSignature = observation.requestSignature();
            }
        }
        return List.copyOf(compact);
    }

    /** Returns the first observation for each signature plus its occurrence count. */
    public Map<String, Integer> signatureCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        observations.forEach(item -> counts.merge(item.requestSignature(), 1, Integer::sum));
        return Collections.unmodifiableMap(new LinkedHashMap<>(counts));
    }

    /** Searches the entire journal; use {@link #awaitAfter} for action read-back. */
    public Optional<UiNetworkObservation> await(Predicate<UiNetworkObservation> predicate,
                                                 Duration timeout) {
        return awaitAfter(0, predicate, timeout);
    }

    /** Waits only for observations captured after a cursor taken before the action. */
    public Optional<UiNetworkObservation> awaitAfter(int cursor,
                                                     Predicate<UiNetworkObservation> predicate,
                                                     Duration timeout) {
        Objects.requireNonNull(predicate, "predicate");
        if (!configuration.settlementPolicy().enabled()) {
            throw new IllegalStateException(
                    "UI network settlement is disabled; enable it explicitly before awaiting read-back");
        }
        if (cursor < 0 || cursor > observations.size()) {
            throw new IllegalArgumentException("UI network observation cursor is outside this journal");
        }
        Duration effective = timeout == null ? configuration.settlementPolicy().timeout() : timeout;
        if (effective.isZero() || effective.isNegative()) {
            throw new IllegalArgumentException("ui.network.await timeout must be positive");
        }
        long deadline = System.nanoTime() + effective.toNanos();
        int inspected = cursor;
        while (System.nanoTime() < deadline) {
            List<UiNetworkObservation> current = observations();
            for (int index = inspected; index < current.size(); index++) {
                UiNetworkObservation item = current.get(index);
                if (predicate.test(item)) return Optional.of(item);
            }
            inspected = current.size();
            if (page != null) {
                page.waitForTimeout(Math.max(1, configuration.settlementPolicy()
                        .pollInterval().toMillis()));
            } else {
                Thread.yield();
            }
        }
        return Optional.empty();
    }

    @Override public void close() {
        Throwable failure = release(null);
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
    }

    private Throwable release(Throwable primary) {
        Page owner;
        Consumer<Request> finished, failed;
        synchronized (captureLock) {
            if (closed) return primary;
            closed = true;
            attaching = false;
            owner = page;
            finished = requestFinishedListener;
            failed = requestFailedListener;
            page = null;
            requestFinishedListener = null;
            requestFailedListener = null;
            subscribers.clear();
        }
        primary = release(primary, requestStarts::close);
        if (owner != null && finished != null) primary = release(primary, () -> owner.offRequestFinished(finished));
        if (owner != null && failed != null) primary = release(primary, () -> owner.offRequestFailed(failed));
        return primary;
    }

    private static Throwable release(Throwable primary, Runnable removal) {
        try { removal.run(); }
        catch (RuntimeException | Error failure) {
            if (primary == null) return failure;
            if (primary != failure) primary.addSuppressed(failure);
        }
        return primary;
    }

    private boolean acceptsCaptures() {
        synchronized (captureLock) { return !closed && !attaching; }
    }

    private void requireOpenAttachment() {
        if (closed) throw new IllegalStateException("UI network inspector closed during attachment");
    }

    private void observeFinished(Request request) {
        if (request == null) return;
        if (!acceptsCaptures()) { requestStarts.forget(request); return; }
        try {
            captureSelected(request.url(), UiNetworkCapturePolicy.ResourceKind.fromPlaywright(request.resourceType()), next -> {
                Response response = request.response();
                if (response == null) throw new IllegalStateException("Completed request has no response");
                return started(UiNetworkObservation.from(response, next, configuration), request);
            });
        } catch (RuntimeException transientBrowserState) {
            // Network evidence is diagnostic and must never abort the UI action
            // when the page replaces its execution context during navigation.
        } finally {
            requestStarts.forget(request);
        }
    }

    private void observeFailure(Request request) {
        if (request == null) return;
        if (!acceptsCaptures()) { requestStarts.forget(request); return; }
        try {
            captureSelected(request.url(), UiNetworkCapturePolicy.ResourceKind.fromPlaywright(request.resourceType()),
                    next -> {
                        Response response = request.response();
                        if (isAbortedAttachment(request.failure(), response)) {
                            return started(UiNetworkObservation.fromAttachmentResponse(response, next, configuration), request);
                        }
                        return started(UiNetworkObservation.fromFailure(request, next, configuration), request);
                    });
        } catch (RuntimeException transientBrowserState) {
            // Evidence collection cannot replace the UI action's original outcome.
        } finally {
            requestStarts.forget(request);
        }
    }

    private static boolean isAbortedAttachment(String failure, Response response) {
        if (response == null || response.status() < 200 || response.status() >= 300
                || failure == null || !failure.toUpperCase(java.util.Locale.ROOT).contains("ERR_ABORTED")) {
            return false;
        }
        return response.headers().entrySet().stream()
                .filter(entry -> "content-disposition".equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .anyMatch(value -> value.stripLeading().regionMatches(true, 0, "attachment", 0, 10));
    }

    private void notifySubscribers(UiNetworkObservation observation, List<Consumer<UiNetworkObservation>> current) {
        for (int index = 0; index < current.size(); index++) {
            try { current.get(index).accept(observation); }
            catch (RuntimeException error) {
                deliveryFailures.add(new UiNetworkDeliveryFailure(observation.sequence(), index,
                        observation.requestSignature(), error));
            }
        }
    }
}
