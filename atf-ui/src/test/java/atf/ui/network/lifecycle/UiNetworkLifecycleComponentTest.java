package atf.ui.network.lifecycle;

import atf.testkit.AtfComponentTest;
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.config.UiNetworkSettlementPolicy;
import atf.ui.network.UiNetworkInspector;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

/** Page-port failures expose ownership independently of Chromium or renderer cascading cleanup. */
@AtfComponentTest
class UiNetworkLifecycleComponentTest {
    @ParameterizedTest @CsvSource({"offRequest,false", "offRequestFinished,false", "offRequestFailed,false",
            "offRequest,true", "offRequestFinished,true", "offRequestFailed,true"})
    void everyListenerIsDetachedEvenWhenOneRemovalFails(String operation, boolean error) {
        var port = new PagePort();
        port.failures.put(operation, error ? new AssertionError(operation) : new IllegalStateException(operation));
        var inspector = inspector();
        var deliveries = new AtomicInteger();
        inspector.attach(port.page(), ignored -> deliveries.incrementAndGet());
        var callbacks = new ArrayList<>(port.listeners.values());
        var failure = assertThrows(Throwable.class, inspector::close);
        assertSame(port.failures.get(operation), failure, "The original removal failure retains its identity");
        assertEquals(List.of("offRequest", "offRequestFinished", "offRequestFailed"), port.removals);
        inspector.close();
        assertEquals(3, port.removals.size(), "A second close does not replay removals");
        callbacks.forEach(callback -> callback.accept(port.request()));
        assertEquals(0, port.requestReads.get(), "Retained native callbacks are inert after logical close");
        assertEquals(0, deliveries.get(), "No closed-scope subscriber receives a later callback");
        assertTrue(inspector.observations().isEmpty());
        assertTrue(inspector.capture(port.response()).isEmpty(), "Explicit capture cannot reopen the scope");
        assertThrows(IllegalStateException.class, () -> inspector.subscribe(ignored -> { }));
        assertThrows(IllegalStateException.class, () -> inspector.attach(port.page()));
    }

    @Test void distinctAndIdenticalCleanupCausesNeverMaskTheFirstFailure() {
        for (boolean identical : List.of(false, true)) {
            var port = new PagePort();
            var primary = new AssertionError("first removal");
            port.failures.put("offRequest", primary);
            port.failures.put("offRequestFinished", identical ? primary : new IllegalStateException("second removal"));
            port.failures.put("offRequestFailed", identical ? primary : new AssertionError("third removal"));
            var inspector = inspector(); inspector.attach(port.page());
            assertSame(primary, assertThrows(Throwable.class, inspector::close));
            assertEquals(identical ? List.of() : List.of(port.failures.get("offRequestFinished"),
                    port.failures.get("offRequestFailed")), List.of(primary.getSuppressed()));
            assertEquals(3, port.removals.size(), "All owned cleanup attempts survive Error and RuntimeException");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"onRequest", "onRequestFinished", "onRequestFailed"})
    void partialAttachmentRollsBackEveryAttemptedRegistration(String operation) {
        var port = new PagePort();
        var primary = new IllegalStateException("attach " + operation);
        port.failures.put(operation, primary);
        var inspector = inspector();
        assertSame(primary, assertThrows(Throwable.class, () -> inspector.attach(port.page())));
        assertEquals(port.registrations.stream().map(name -> name.replace("on", "off")).toList(), port.removals);
        assertTrue(port.listeners.isEmpty(), "Even registration which throws after its side effect is rolled back");
        inspector.close();
        assertEquals(port.registrations.size(), port.removals.size());
    }

    @Test void attachmentFailureRemainsPrimaryWhenItsRollbackAlsoFailsWithTheSameOrAnotherCause() {
        for (boolean identical : List.of(false, true)) {
            var port = new PagePort();
            var primary = new AssertionError("partial attach");
            Throwable cleanup = identical ? primary : new IllegalStateException("rollback");
            port.failures.put("onRequestFailed", primary); port.failures.put("offRequest", cleanup);
            var inspector = inspector();
            assertSame(primary, assertThrows(Throwable.class, () -> inspector.attach(port.page())));
            assertEquals(identical ? List.of() : List.of(cleanup), List.of(primary.getSuppressed()));
            assertEquals(3, port.removals.size());
        }
    }

    @Test void closingDuringBodyCaptureCannotRetainOrDeliverTheInFlightObservation() {
        var inspector = inspector(); var port = new PagePort(); var deliveries = new AtomicInteger();
        inspector.attach(port.page(), ignored -> deliveries.incrementAndGet());
        port.onBody = inspector::close;
        assertTrue(inspector.capture(port.response()).isEmpty());
        assertEquals(0, deliveries.get()); assertTrue(inspector.observations().isEmpty());
        assertEquals(0, inspector.statistics().inFlight()); assertEquals(1, inspector.statistics().dropped());
    }

    @Test void closingInsideDeliveryPreservesTheAlreadyStartedSubscriberSnapshot() {
        var inspector = inspector(); var port = new PagePort(); var deliveries = new AtomicInteger();
        inspector.attach(port.page(), ignored -> inspector.close());
        inspector.subscribe(ignored -> deliveries.incrementAndGet());
        assertTrue(inspector.capture(port.response()).isPresent());
        assertEquals(1, deliveries.get(), "Close does not cancel the same observation's remaining healthy subscribers");
        assertTrue(inspector.capture(port.response()).isEmpty());
        assertEquals(1, deliveries.get(), "A subsequent capture cannot reach that completed subscriber snapshot");
        assertEquals(1, inspector.observations().size(), "Previously retained evidence remains auditable after close");
    }

    private static UiNetworkInspector inspector() {
        return new UiNetworkInspector(new UiNetworkInspectionConfiguration(true,
                Set.of("fixture.test"), 8, 256, true, UiNetworkSettlementPolicy.disabled()));
    }

    private static final class PagePort {
        final Map<String, Throwable> failures = new LinkedHashMap<>();
        final Map<String, Consumer<Request>> listeners = new LinkedHashMap<>();
        final List<String> registrations = new ArrayList<>(), removals = new ArrayList<>();
        final AtomicInteger requestReads = new AtomicInteger();
        Runnable onBody = () -> { };
        Page page() { return proxy(Page.class, (name, args) -> {
            if (name.startsWith("onRequest")) {
                registrations.add(name); listeners.put(name, callback(args));
            } else if (name.startsWith("offRequest")) {
                removals.add(name);
                if (!failures.containsKey(name)) listeners.remove(name.replace("off", "on"));
            }
            if (failures.containsKey(name)) throw failures.get(name);
            return null;
        }); }
        Request request() { return proxy(Request.class, (name, args) -> {
            requestReads.incrementAndGet();
            return switch (name) {
                case "url" -> "https://fixture.test/value";
                case "resourceType" -> "xhr";
                case "method" -> "GET";
                case "headers" -> Map.of();
                case "response" -> response();
                default -> null;
            };
        }); }
        Response response() { return proxy(Response.class, (name, args) -> switch (name) {
            case "url" -> "https://fixture.test/value";
            case "request" -> request();
            case "status" -> 200;
            case "headers" -> Map.of("content-type", "text/plain");
            case "body" -> { onBody.run(); yield "value".getBytes(java.nio.charset.StandardCharsets.UTF_8); }
            default -> null;
        }); }
        @SuppressWarnings("unchecked") private static Consumer<Request> callback(Object[] args) { return (Consumer<Request>) args[0]; }
    }
    @FunctionalInterface private interface Invocation { Object call(String name, Object[] args) throws Throwable; }
    private static <T> T proxy(Class<T> type, Invocation call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (instance, method, args) -> call.call(method.getName(), args)));
    }
}
