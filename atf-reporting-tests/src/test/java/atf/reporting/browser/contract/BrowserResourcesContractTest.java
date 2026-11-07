package atf.reporting.browser.contract;

import atf.reporting.browser.junit.ReportBrowserTest;
import atf.reporting.browser.lifecycle.BrowserProbeBudget;
import atf.reporting.browser.lifecycle.BrowserProbeScope;
import atf.reporting.browser.process.BrowserPhaseDeadline;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.lifecycle.BrowserResources;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Failure-path ownership without an SDK download or a live application. */
@AtfComponentTest
@ReportBrowserTest
class BrowserResourcesContractTest {
    @Test void verificationRemainsPrimaryAndEveryRemainingResourceIsAttemptedOnce() {
        var calls = new ArrayList<String>();
        var original = new AssertionError("original verification");
        var cleanup = new IllegalStateException("first disposal");
        AutoCloseable first = BrowserResources.create(() -> () -> { calls.add("first"); throw cleanup; });
        AutoCloseable second = BrowserResources.create(() -> () -> calls.add("second"));
        var result = assertThrows(AssertionError.class, () -> BrowserResources.finish(() -> { throw original; }, first, second));
        assertSame(original, result);
        assertArrayEquals(new Throwable[] {cleanup}, result.getSuppressed());
        assertEquals(java.util.List.of("first", "second"), calls);
        BrowserResources.closeAll(first, second);
        BrowserResources.closeRemaining();
        assertEquals(java.util.List.of("first", "second"), calls);
    }

    @Test void aPageLeaseExplicitlyDisposesItsPageBeforeItsPrivateContext() {
        var calls = new ArrayList<String>();
        var page = sdk(com.microsoft.playwright.Page.class, calls, "page", null);
        var context = sdk(com.microsoft.playwright.BrowserContext.class, calls, "context", page);
        var browser = BrowserResources.create(() -> sdk(com.microsoft.playwright.Browser.class, calls, "browser", context));
        var acquired = BrowserResources.newPage(browser);
        assertSame(page, acquired, "The boundary returns the exact SDK value");
        BrowserResources.closeAll(acquired, browser);
        assertEquals(java.util.List.of("page", "context", "browser"), calls);
        BrowserResources.closeAll(acquired, context, browser);
        assertEquals(java.util.List.of("page", "context", "browser"), calls);
    }

    @Test void publicationKeepsTheCallerPhaseAfterSdkDisposal() {
        var phases = new ArrayList<String>();
        AutoCloseable sdk = BrowserResources.create(() -> () -> phases.add(currentPhase()));
        AutoCloseable publication = () -> phases.add(currentPhase());
        BrowserResources.closeAll(sdk, publication);
        assertEquals(java.util.List.of("CLEANUP", "VERIFY"), phases,
                "Non-SDK publication must not consume the SDK disposal deadline");
    }

    private static String currentPhase() throws Exception {
        return currentEvent().substring(15);
    }

    private static String currentEvent() throws Exception {
        try (var files = java.nio.file.Files.list(BrowserProbeScope.directory())) {
            String event = files.map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("phase-") && name.length() > 15 && Character.isDigit(name.charAt(6)))
                    .max(String::compareTo).orElseThrow();
            return event;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T sdk(Class<T> type, ArrayList<String> calls, String name, Object child) {
        return sdk(type, calls, name, child, event -> { });
    }

    @SuppressWarnings("unchecked")
    private static <T> T sdk(Class<T> type, ArrayList<String> calls, String name, Object child, java.util.function.Consumer<String> onClose) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, arguments) -> {
            return switch (method.getName()) {
                case "newPage", "newContext" -> child;
                case "close" -> { calls.add(name); onClose.accept(currentEvent()); yield null; }
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == arguments[0];
                case "toString" -> name;
                default -> throw new AssertionError("Unexpected controlled SDK call: " + method);
            };
        });
    }

    @Test void aFailedAcquisitionDoesNotLoseAnEarlierOwnedResource() {
        var calls = new ArrayList<String>();
        BrowserResources.create(() -> (AutoCloseable) () -> calls.add("retained"));
        var original = new IllegalArgumentException("factory failed");
        assertSame(original, assertThrows(IllegalArgumentException.class,
                () -> BrowserResources.create(() -> { throw original; })));
        BrowserResources.closeRemaining();
        assertEquals(java.util.List.of("retained"), calls);
    }

    @Test void equalButDistinctResourcesCannotSuppressEachOthersCleanup() {
        var calls = new ArrayList<String>();
        class EqualResource implements AutoCloseable {
            private final String name;
            EqualResource(String name) { this.name = name; }
            @Override public void close() { calls.add(name); }
            @Override public boolean equals(Object other) { return other instanceof EqualResource; }
            @Override public int hashCode() { return 1; }
        }
        var first = BrowserResources.create(() -> new EqualResource("first"));
        var second = BrowserResources.create(() -> new EqualResource("second"));
        assertNotSame(first, second);
        assertEquals(first, second, "Value equality must not merge distinct owned resources");
        BrowserResources.closeAll(first, second);
        BrowserResources.closeAll(first, second);
        BrowserResources.closeRemaining();
        assertEquals(java.util.List.of("first", "second"), calls);
    }

    @Test void eachNativeDisposalKeepsItsOwnDeadlineWithinTheJunitBody() {
        assertEquals(java.util.List.of(false, false, false), observeDisposalDeadlines(false));
    }

    @Test void anExistingGlobalCleanupDeadlineCannotBeRenewedByNestedResources() {
        assertEquals(java.util.List.of(false, true, true), observeDisposalDeadlines(true));
    }

    private static java.util.List<Boolean> observeDisposalDeadlines(boolean globalCleanup) {
        var clock = new java.util.concurrent.atomic.AtomicLong();
        var deadline = new BrowserPhaseDeadline(clock::get);
        var expired = new ArrayList<Boolean>();
        java.util.function.Consumer<String> capture = event -> {
            deadline.observe(event);
            clock.addAndGet(java.time.Duration.ofSeconds(4).toNanos());
            expired.add(deadline.expired(BrowserProbeBudget.DEFAULT));
        };
        var calls = new ArrayList<String>();
        var page = sdk(com.microsoft.playwright.Page.class, calls, "page", null, capture);
        var context = sdk(com.microsoft.playwright.BrowserContext.class, calls, "context", page, capture);
        var browser = BrowserResources.create(() -> sdk(com.microsoft.playwright.Browser.class, calls, "browser", context, capture));
        BrowserResources.newPage(browser);
        try (var root = globalCleanup ? BrowserProbeScope.cleanup() : null) {
            BrowserResources.closeAll(browser);
        }
        assertEquals(java.util.List.of("page", "context", "browser"), calls);
        return expired;
    }
}
