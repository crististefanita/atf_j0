package atf.reporting.audit;

import atf.testkit.AtfComponentTest;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.security.SensitiveDataRedactor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Session ownership and scenario isolation; consumer/JUnit adapters are tested at their owners. */
@AtfComponentTest
class AssertionAuditRunComponentTest {
    @Test void dedicatedPublicationKeepsItsThreadFailureIdentityAndInterruptFlag() {
        try (var executor = AssertionAuditRun.PublicationExecutor.dedicated()) {
            long caller = Thread.currentThread().threadId();
            long owner = executor.call(() -> Thread.currentThread().threadId());
            assertNotEquals(caller, owner);
            assertEquals(owner, executor.call(() -> Thread.currentThread().threadId()));
            var failure = new IllegalStateException("controlled publication failure");
            var attempts = new AtomicInteger();
            assertSame(failure, assertThrows(IllegalStateException.class, () -> executor.run(() -> {
                attempts.incrementAndGet(); throw failure;
            })));
            assertEquals(1, attempts.get());
            Thread.currentThread().interrupt();
            try {
                assertEquals(42, executor.call(() -> 42));
                assertTrue(Thread.currentThread().isInterrupted());
            } finally { Thread.interrupted(); }
        }
    }

    @Test void borrowedScenariosKeepDuplicateTitlesAndOneOwnedSession(@TempDir Path output) {
        var closes = new AtomicInteger();
        var session = new HtmlReportSession(output.resolve("combined.html"));
        var run = new AssertionAuditRun(counted(session, closes), new SensitiveDataRedactor());
        write(run, "First", "same visible title", "one", true);
        write(run, "First", "same visible title", "two", false);
        write(run, "Second", "third case", "three", true);
        assertEquals(0, closes.get(), "borrowed scenario disposal must not publish the parent");
        var snapshot = session.semanticSnapshot();
        assertEquals(2, snapshot.classes().size());
        assertEquals(3, snapshot.scenarioCount());
        assertEquals(2, snapshot.classes().getFirst().scenarios().size());
        assertEquals(AtfReportStatus.FAIL, snapshot.classes().getFirst().scenarios().get(1).status());
        assertEquals(0, snapshot.requestCount(), "JUnit assertions must not fabricate HTTP exchanges");
        run.close(); run.close();
        assertEquals(1, closes.get());
        assertThrows(IllegalStateException.class, () -> run.open(identity("Late", "late", "late")));
    }

    @Test void aBorrowedOpenFailureLeavesTheOwnerUsableAndOwnedConstructionStillCleansUp() {
        var closes = new AtomicInteger();
        var primary = new IllegalStateException("scenario acquisition failed");
        AtfReportPorts.Session broken = broken(primary, closes, null);
        var run = new AssertionAuditRun(broken, new SensitiveDataRedactor());
        assertSame(primary, assertThrows(IllegalStateException.class,
                () -> run.open(identity("Broken", "case", "method"))));
        assertEquals(0, closes.get());
        run.close(); assertEquals(1, closes.get());

        var secondary = new IllegalStateException("session close failed");
        var ownedPrimary = new IllegalStateException("owned acquisition failed");
        assertSame(ownedPrimary, assertThrows(IllegalStateException.class,
                () -> new AssertionAuditReport(broken(ownedPrimary, new AtomicInteger(), secondary),
                        identity("Owned", "case", "method"), new SensitiveDataRedactor())));
        assertArrayEquals(new Throwable[]{secondary}, ownedPrimary.getSuppressed());
    }

    @Test void aFailingOwnerCloseIsNeverReplayed() {
        var closes = new AtomicInteger();
        var failure = new IllegalStateException("controlled owner failure");
        var run = new AssertionAuditRun(broken(new IllegalStateException(), closes, failure), new SensitiveDataRedactor());
        assertSame(failure, assertThrows(IllegalStateException.class, run::close));
        run.close(); assertEquals(1, closes.get());
    }

    @Test void concurrentInvocationsDoNotSharePhaseOrAssertionState(@TempDir Path output) throws Exception {
        var session = new HtmlReportSession(output.resolve("concurrent.html"));
        try (var run = new AssertionAuditRun(session, new SensitiveDataRedactor());
             var workers = Executors.newFixedThreadPool(4)) {
            var tasks = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int index = 0; index < 8; index++) {
                final int number = index;
                tasks.add(workers.submit(() -> write(run, "Concurrent", "case " + number, "method " + number, true)));
            }
            for (var task : tasks) task.get();
            assertEquals(8, session.semanticSnapshot().scenarioCount());
            assertTrue(session.semanticSnapshot().classes().getFirst().scenarios().stream().allMatch(scenario ->
                    scenario.phases().size() == 1 && scenario.phases().getFirst().nodes().size() == 1
                            && scenario.status() == AtfReportStatus.PASS));
        }
    }

    @Test void standaloneProjectionStillOwnsItsSession(@TempDir Path output) {
        var closes = new AtomicInteger();
        var report = new AssertionAuditReport(counted(new HtmlReportSession(output.resolve("owned.html")), closes),
                identity("Standalone", "one case", "method"), new SensitiveDataRedactor());
        report.phase("TEST BODY", List.of(new AtfAssertion("value", "1", "1", true)), AtfReportStatus.PASS, "");
        report.complete(AtfReportStatus.PASS);
        report.close(); report.close();
        assertEquals(1, closes.get());
    }

    private static void write(AssertionAuditRun run, String owner, String title, String method, boolean passed) {
        try (var report = run.open(identity(owner, title, method))) {
            var status = passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
            report.phase("TEST BODY", List.of(new AtfAssertion(method, "ready", passed ? "ready" : "missing", passed)), status, "");
            report.complete(status);
        }
    }
    private static AssertionAuditReport.Identity identity(String owner, String title, String method) {
        return new AssertionAuditReport.Identity(owner, "fixture.audit", title, method, List.of("component"));
    }
    private static AtfReportPorts.Session counted(AtfReportPorts.Session session, AtomicInteger closes) {
        return (AtfReportPorts.Session) Proxy.newProxyInstance(AtfReportPorts.Session.class.getClassLoader(),
                new Class<?>[]{AtfReportPorts.Session.class}, (proxy, method, args) -> {
                    if (method.getName().equals("close")) closes.incrementAndGet();
                    try { return method.invoke(session, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }
    private static AtfReportPorts.Session broken(RuntimeException primary, AtomicInteger closes, RuntimeException closeFailure) {
        return (AtfReportPorts.Session) Proxy.newProxyInstance(AtfReportPorts.Session.class.getClassLoader(),
                new Class<?>[]{AtfReportPorts.Session.class}, (proxy, method, args) -> {
                    if (method.getName().equals("startScenario")) throw primary;
                    if (method.getName().equals("close")) {
                        closes.incrementAndGet(); if (closeFailure != null) throw closeFailure;
                    }
                    return null;
                });
    }
}
