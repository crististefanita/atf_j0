package atf.testkit.assertions;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.testkit.scope.TestRunScope;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** Actual Jupiter owners; no report types, browser or consumer dependency. */
@AtfComponentTest
class ScopedComponentAuditExtensionComponentTest {
    private static final String PROBE = "atf.fixture.scoped-audit";
    private static final Map<String, Probe> RUNS = new ConcurrentHashMap<>();
    private static final class Probe {
        final AtomicInteger opened = new AtomicInteger(), closed = new AtomicInteger();
        final List<ComponentAssertionAudit.Result> results = new CopyOnWriteArrayList<>();
        final boolean failClose;
        boolean failPublication;
        TestExecutionSummary summary;
        Probe(boolean failClose) { this.failClose = failClose; }
    }

    @Test void suiteSharesOneResourceButKeepsIndependentAudits() {
        var probe = launch("suite", false, FirstFixture.class, SecondFixture.class);
        AuditedAssertions.assertEquals(3, probe.summary.getTestsSucceededCount());
        AuditedAssertions.assertEquals(1, probe.opened.get());
        AuditedAssertions.assertEquals(1, probe.closed.get());
        AuditedAssertions.assertEquals(3, probe.results.size());
        AuditedAssertions.assertTrue(probe.results.stream().allMatch(result -> result.phases().size() == 1
                && result.phases().getFirst().checks().size() == 1));
    }

    @Test void defaultClassOwnershipAndRepeatedLaunchesRemainIndependent() {
        var first = launch(null, false, FirstFixture.class, SecondFixture.class);
        var second = launch(null, false, FirstFixture.class);
        AuditedAssertions.assertEquals(2, first.opened.get());
        AuditedAssertions.assertEquals(2, first.closed.get());
        AuditedAssertions.assertEquals(1, second.opened.get());
        AuditedAssertions.assertEquals(1, second.closed.get());
    }

    @Test void classSetupFailureHasADiagnosticWithoutInventedChecks() {
        var probe = launch("suite", false, BeforeAllFixture.class);
        AuditedAssertions.assertEquals(1, probe.summary.getContainersFailedCount());
        AuditedAssertions.assertEquals(0, probe.summary.getTestsStartedCount());
        AuditedAssertions.assertEquals(1, probe.results.size());
        var phase = probe.results.getFirst().phases().getFirst();
        AuditedAssertions.assertEquals(ComponentAssertionAudit.Phase.SETUP, phase.phase());
        AuditedAssertions.assertEquals(ComponentAssertionAudit.Outcome.FAIL, phase.outcome());
        AuditedAssertions.assertTrue(phase.checks().isEmpty());
        AuditedAssertions.assertTrue(phase.diagnostic().contains("class setup failed"));
        AuditedAssertions.assertEquals(1, probe.closed.get());
    }

    @Test void classTeardownAndResourceCloseFailuresDoNotRerunBodies() {
        var teardown = launch("suite", false, AfterAllFixture.class);
        AuditedAssertions.assertEquals(1, teardown.summary.getTestsSucceededCount());
        AuditedAssertions.assertEquals(1, teardown.summary.getContainersFailedCount());
        AuditedAssertions.assertEquals(2, teardown.results.size());
        AuditedAssertions.assertEquals(ComponentAssertionAudit.Phase.TEARDOWN,
                teardown.results.getLast().phases().getFirst().phase());
        var close = launch("suite", true, FirstFixture.class, SecondFixture.class);
        AuditedAssertions.assertEquals(3, close.summary.getTestsSucceededCount());
        AuditedAssertions.assertEquals(1, close.summary.getContainersFailedCount());
        AuditedAssertions.assertEquals(1, close.closed.get());
        AuditedAssertions.assertEquals(3, close.results.size());
    }

    @Test void anAbortedClassDoesNotHideAPublicationFailure() {
        var probe = launch("suite", false, true, AbortedClassFixture.class);
        AuditedAssertions.assertEquals(1, probe.summary.getContainersFailedCount());
        AuditedAssertions.assertEquals(0, probe.summary.getContainersAbortedCount());
        AuditedAssertions.assertEquals(1, probe.results.size());
        AuditedAssertions.assertEquals(ComponentAssertionAudit.Outcome.ABORTED, probe.results.getFirst().outcome());
        AuditedAssertions.assertTrue(probe.summary.getFailures().getFirst().getException().getMessage().contains("publication failed"));
        AuditedAssertions.assertEquals(1, probe.closed.get());
    }

    private static Probe launch(String scope, boolean failClose, Class<?>... fixtures) {
        return launch(scope, failClose, false, fixtures);
    }

    private static Probe launch(String scope, boolean failClose, boolean failPublication, Class<?>... fixtures) {
        var probe = new Probe(failClose);
        probe.failPublication = failPublication;
        String id = UUID.randomUUID().toString(); RUNS.put(id, probe);
        try {
            var request = LauncherDiscoveryRequestBuilder.request().configurationParameter(PROBE, id)
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false");
            if (scope != null) request.configurationParameter(TestRunScope.CONFIGURATION_PARAMETER, scope);
            for (Class<?> fixture : fixtures) request.selectors(selectClass(fixture));
            var listener = new SummaryGeneratingListener(); LauncherFactory.create().execute(request.build(), listener);
            probe.summary = listener.getSummary(); return probe;
        } finally { RUNS.remove(id); }
    }

    private record Owner(Probe probe) implements AutoCloseable {
        @Override public void close() {
            probe.closed.incrementAndGet();
            if (probe.failClose) throw new IllegalStateException("controlled owner close failure");
        }
    }

    public static final class Adapter implements ComponentAuditAdapter {
        private final ComponentAuditExtension audit = ComponentAuditExtension.scoped(Adapter.class, Owner.class,
                context -> {
                    Probe probe = RUNS.get(context.getConfigurationParameter(PROBE).orElseThrow());
                    probe.opened.incrementAndGet(); return new Owner(probe);
                }, (owner, context) -> new ComponentAuditExtension.Publication() {
                    public AssertionValueDisplay display() { return new AssertionValueDisplay(240, (field, value) -> value); }
                    public void publish(ComponentAssertionAudit.Result result) {
                        owner.probe.results.add(result);
                        if (owner.probe.failPublication) throw new IllegalStateException("publication failed");
                    }
                    public void close() { }
                });
        public ComponentAuditExtension auditExtension() { return audit; }
    }

    @ExtendWith(Adapter.class) static class FirstFixture {
        @Test void first() { AuditedAssertions.assertEquals(1, 1); }
        @Test void second() { AuditedAssertions.assertEquals(2, 2); }
    }
    @ExtendWith(Adapter.class) static class SecondFixture {
        @Test void third() { AuditedAssertions.assertEquals(3, 3); }
    }
    @ExtendWith(Adapter.class) static class BeforeAllFixture {
        @BeforeAll static void setup() { throw new IllegalStateException("class setup failed"); }
        @Test void unreachable() { fail("setup must stop execution"); }
    }
    @ExtendWith(Adapter.class) static class AfterAllFixture {
        @Test void body() { AuditedAssertions.assertTrue(true); }
        @AfterAll static void teardown() { throw new IllegalStateException("class teardown failed"); }
    }
    @ExtendWith(Adapter.class) static class AbortedClassFixture {
        @BeforeAll static void setup() { Assumptions.assumeTrue(false, "class assumption"); }
        @Test void unreachable() { fail("aborted setup must stop execution"); }
    }
}
